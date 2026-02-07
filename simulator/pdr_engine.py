"""
Python PDR engine — faithful port of the Kotlin implementation.
Classes: StepDetector, OrientationEstimator, KalmanFilter, PDREngine
"""

import math
import numpy as np
from dataclasses import dataclass, field
from typing import List, Callable, Optional


# ---------------------------------------------------------------------------
# Data helpers
# ---------------------------------------------------------------------------

@dataclass
class SensorData:
    timestamp: int  # nanoseconds
    accX: float = 0.0
    accY: float = 0.0
    accZ: float = 0.0
    gyroX: float = 0.0
    gyroY: float = 0.0
    gyroZ: float = 0.0
    magX: float = 0.0
    magY: float = 0.0
    magZ: float = 0.0


@dataclass
class StepEvent:
    step_length: float
    timestamp: int


# ---------------------------------------------------------------------------
# StepDetector  (peak-detection + Weinberg)
# ---------------------------------------------------------------------------

class StepDetector:
    def __init__(self, weinberg_k: float = 0.5, peak_threshold: float = 1.2,
                 min_step_interval_ns: int = 300_000_000):
        self.weinberg_k = weinberg_k
        self.peak_threshold = peak_threshold
        self.min_step_interval = min_step_interval_ns
        self.step_count = 0
        self._last_step_time = 0
        self._window: List[float] = []
        self._window_size = 30
        self._listener: Optional[Callable[[StepEvent], None]] = None

    def set_on_step_listener(self, fn: Callable[[StepEvent], None]):
        self._listener = fn

    def on_accelerometer_update(self, x: float, y: float, z: float, timestamp: int):
        mag = math.sqrt(x * x + y * y + z * z)
        self._window.append(mag)
        if len(self._window) > self._window_size:
            self._window.pop(0)
        if len(self._window) < self._window_size:
            return

        a_max = max(self._window)
        a_min = min(self._window)
        mid = len(self._window) // 2
        mid_val = self._window[mid]

        is_peak = (mid_val == a_max) and ((a_max - 9.81) > self.peak_threshold)

        if is_peak and (timestamp - self._last_step_time) > self.min_step_interval:
            self._last_step_time = timestamp
            self.step_count += 1
            step_length = self.weinberg_k * ((a_max - a_min) ** 0.25)
            if self._listener:
                self._listener(StepEvent(step_length, timestamp))

    def reset(self):
        self.step_count = 0
        self._window.clear()
        self._last_step_time = 0


# ---------------------------------------------------------------------------
# OrientationEstimator  (complementary filter)
# ---------------------------------------------------------------------------

class OrientationEstimator:
    def __init__(self, alpha: float = 0.98):
        self.alpha = alpha
        self.heading: float = 0.0  # radians, 0=north CW
        self._last_timestamp: int = 0
        self._initialized = False

    def update(self, data: SensorData):
        mag_heading = self._compute_mag_heading(data)

        if not self._initialized:
            self.heading = mag_heading
            self._last_timestamp = data.timestamp
            self._initialized = True
            return

        dt = (data.timestamp - self._last_timestamp) * 1e-9
        self._last_timestamp = data.timestamp

        if dt <= 0 or dt > 1.0:
            self.heading = mag_heading
            return

        gyro_heading = self.heading + data.gyroZ * dt

        # Complementary filter with proper angular wrapping
        diff = mag_heading - gyro_heading
        # Normalize diff to [-π, π)
        diff = (diff + math.pi) % (2 * math.pi) - math.pi
        self.heading = gyro_heading + (1 - self.alpha) * diff
        # Normalize to [0, 2π)
        self.heading = self.heading % (2 * math.pi)
        if self.heading < 0:
            self.heading += 2 * math.pi

    @staticmethod
    def _compute_mag_heading(data: SensorData) -> float:
        """Simplified: use atan2(magY, magX) — same fallback as Kotlin."""
        h = math.atan2(data.magX, data.magY)  # heading: 0=north
        if h < 0:
            h += 2 * math.pi
        return h

    def reset(self):
        self.heading = 0.0
        self._initialized = False
        self._last_timestamp = 0


# ---------------------------------------------------------------------------
# KalmanFilter (1-D)
# ---------------------------------------------------------------------------

class KalmanFilter:
    def __init__(self, process_noise: float = 0.01, measurement_noise: float = 0.1):
        self.Q = process_noise
        self.R = measurement_noise
        self._estimate = 0.0
        self._error_cov = 1.0
        self._initialized = False

    def update(self, measurement: float) -> float:
        if not self._initialized:
            self._estimate = measurement
            self._error_cov = 1.0
            self._initialized = True
            return self._estimate
        predicted_error = self._error_cov + self.Q
        K = predicted_error / (predicted_error + self.R)
        self._estimate += K * (measurement - self._estimate)
        self._error_cov = (1 - K) * predicted_error
        return self._estimate

    def reset(self):
        self._estimate = 0.0
        self._error_cov = 1.0
        self._initialized = False


# ---------------------------------------------------------------------------
# PDREngine
# ---------------------------------------------------------------------------

class PDREngine:
    def __init__(self):
        self.step_detector = StepDetector()
        self.orientation_estimator = OrientationEstimator()
        self.pos_x = 0.0  # east
        self.pos_z = 0.0  # north  (note: Kotlin uses Z=south for screen coords; we keep north-positive for plotting)
        self.trajectory: List[tuple] = [(0.0, 0.0)]

        def _on_step(event: StepEvent):
            heading = self.orientation_estimator.heading
            self.pos_x += event.step_length * math.sin(heading)
            self.pos_z += event.step_length * math.cos(heading)
            self.trajectory.append((self.pos_x, self.pos_z))

        self.step_detector.set_on_step_listener(_on_step)

    def on_sensor_update(self, data: SensorData):
        self.step_detector.on_accelerometer_update(data.accX, data.accY, data.accZ, data.timestamp)
        self.orientation_estimator.update(data)

    def reset(self):
        self.pos_x = 0.0
        self.pos_z = 0.0
        self.trajectory = [(0.0, 0.0)]
        self.step_detector.reset()
        self.orientation_estimator.reset()

    @property
    def position(self):
        return (self.pos_x, self.pos_z)
