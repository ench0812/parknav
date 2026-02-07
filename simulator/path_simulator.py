"""
Path simulator — generates realistic IMU sensor streams for known paths.
Sampling rate: 50 Hz (20 ms).  Walking frequency: 2 Hz.
"""

import math
import numpy as np
from typing import List, Tuple
from pdr_engine import SensorData

SAMPLE_RATE = 50        # Hz
DT_NS = int(1e9 / SAMPLE_RATE)
WALK_FREQ = 2.0         # Hz (step frequency)
WALK_AMP = 1.92          # m/s² amplitude; Weinberg K=0.5 → step ≈ 0.7m
GRAVITY = 9.81
MAG_STRENGTH = 20.0

# Noise std devs (defaults)
ACC_NOISE = 0.3
GYRO_NOISE = 0.01
MAG_NOISE = 1.0


def _walk_samples(duration_s: float, heading: float, t0_ns: int = 0,
                  noise_scale: float = 1.0) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    """Generate sensor data for walking at constant heading for *duration_s* seconds."""
    n = int(duration_s * SAMPLE_RATE)
    samples: List[SensorData] = []
    # Ground-truth waypoints: one per step
    gt_points: List[Tuple[float, float]] = []

    for i in range(n):
        t_s = i / SAMPLE_RATE
        ts = t0_ns + i * DT_NS

        # Accelerometer: gravity + walking oscillation along vertical
        osc = WALK_AMP * math.sin(2 * math.pi * WALK_FREQ * t_s)
        acc_mag = GRAVITY + osc
        # Distribute on axes assuming phone roughly upright
        acc_x = acc_mag * math.sin(heading) * 0.1 + np.random.normal(0, ACC_NOISE * noise_scale)
        acc_y = acc_mag * 0.99 + np.random.normal(0, ACC_NOISE * noise_scale)
        acc_z = acc_mag * math.cos(heading) * 0.1 + np.random.normal(0, ACC_NOISE * noise_scale)

        # Magnetometer
        mag_x = MAG_STRENGTH * math.sin(heading) + np.random.normal(0, MAG_NOISE * noise_scale)
        mag_y = MAG_STRENGTH * math.cos(heading) + np.random.normal(0, MAG_NOISE * noise_scale)
        mag_z = np.random.normal(0, MAG_NOISE * noise_scale)

        # Gyroscope — straight walk, nearly zero yaw rate
        gyro_z = np.random.normal(0, GYRO_NOISE * noise_scale)

        samples.append(SensorData(
            timestamp=ts,
            accX=acc_x, accY=acc_y, accZ=acc_z,
            gyroX=0.0, gyroY=0.0, gyroZ=gyro_z,
            magX=mag_x, magY=mag_y, magZ=mag_z,
        ))

    return samples


def _turn_samples(from_heading: float, to_heading: float, duration_s: float,
                  t0_ns: int = 0, noise_scale: float = 1.0) -> List[SensorData]:
    """Generate sensor data while turning in place (no steps)."""
    n = int(duration_s * SAMPLE_RATE)
    samples: List[SensorData] = []
    delta = to_heading - from_heading
    # Normalize delta
    while delta > math.pi:
        delta -= 2 * math.pi
    while delta < -math.pi:
        delta += 2 * math.pi

    yaw_rate = delta / duration_s if duration_s > 0 else 0.0

    for i in range(n):
        t_s = i / SAMPLE_RATE
        ts = t0_ns + i * DT_NS
        frac = t_s / duration_s if duration_s > 0 else 1.0
        cur_heading = from_heading + delta * frac

        # Stationary accel (gravity only, small walking to allow algorithm to NOT detect steps)
        acc_x = np.random.normal(0, ACC_NOISE * noise_scale)
        acc_y = GRAVITY + np.random.normal(0, ACC_NOISE * noise_scale * 0.3)
        acc_z = np.random.normal(0, ACC_NOISE * noise_scale)

        mag_x = MAG_STRENGTH * math.sin(cur_heading) + np.random.normal(0, MAG_NOISE * noise_scale)
        mag_y = MAG_STRENGTH * math.cos(cur_heading) + np.random.normal(0, MAG_NOISE * noise_scale)
        mag_z = np.random.normal(0, MAG_NOISE * noise_scale)

        gyro_z = yaw_rate + np.random.normal(0, GYRO_NOISE * noise_scale)

        samples.append(SensorData(
            timestamp=ts,
            accX=acc_x, accY=acc_y, accZ=acc_z,
            gyroX=0.0, gyroY=0.0, gyroZ=gyro_z,
            magX=mag_x, magY=mag_y, magZ=mag_z,
        ))
    return samples


def _heading_rad(deg: float) -> float:
    return math.radians(deg)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def simulate_straight(distance: float, heading: float, step_length: float = 0.7,
                      noise_scale: float = 1.0) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    """Returns (sensor_stream, ground_truth_points)."""
    n_steps = distance / step_length
    duration_s = n_steps / WALK_FREQ  # each step = 0.5s at 2Hz
    samples = _walk_samples(duration_s, heading, noise_scale=noise_scale)

    # Ground truth: start(0,0) -> end
    gt = [(0.0, 0.0),
          (distance * math.sin(heading), distance * math.cos(heading))]
    return samples, gt


def simulate_turn(from_heading: float, to_heading: float, duration_s: float = 1.5,
                  t0_ns: int = 0, noise_scale: float = 1.0) -> List[SensorData]:
    return _turn_samples(from_heading, to_heading, duration_s, t0_ns, noise_scale)


def simulate_l_shape(leg1: float, leg2: float, noise_scale: float = 1.0
                     ) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    heading1 = 0.0  # north
    heading2 = math.pi / 2  # east (left turn = +90° CW)

    step_length = 0.7
    dur1 = (leg1 / step_length) / WALK_FREQ
    dur2 = (leg2 / step_length) / WALK_FREQ
    turn_dur = 1.5

    s1 = _walk_samples(dur1, heading1, t0_ns=0, noise_scale=noise_scale)
    t0 = len(s1) * DT_NS
    s_turn = _turn_samples(heading1, heading2, turn_dur, t0_ns=t0, noise_scale=noise_scale)
    t0 += len(s_turn) * DT_NS
    s2 = _walk_samples(dur2, heading2, t0_ns=t0, noise_scale=noise_scale)

    samples = s1 + s_turn + s2
    gt = [(0.0, 0.0),
          (0.0, leg1),
          (leg2, leg1)]
    return samples, gt


def simulate_rectangle(width: float, height: float, noise_scale: float = 1.0
                       ) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    """Walk a rectangle: north -> east -> south -> west -> back to origin."""
    step_length = 0.7
    headings = [0.0, math.pi / 2, math.pi, 3 * math.pi / 2]
    legs = [height, width, height, width]
    turn_dur = 1.5

    samples: List[SensorData] = []
    t0 = 0

    for i, (h, leg) in enumerate(zip(headings, legs)):
        if i > 0:
            prev_h = headings[i - 1]
            st = _turn_samples(prev_h, h, turn_dur, t0_ns=t0, noise_scale=noise_scale)
            samples.extend(st)
            t0 += len(st) * DT_NS

        dur = (leg / step_length) / WALK_FREQ
        sw = _walk_samples(dur, h, t0_ns=t0, noise_scale=noise_scale)
        samples.extend(sw)
        t0 += len(sw) * DT_NS

    gt = [(0, 0), (0, height), (width, height), (width, 0), (0, 0)]
    return samples, gt


def simulate_stationary(duration_s: float, noise_scale: float = 1.0
                        ) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    n = int(duration_s * SAMPLE_RATE)
    samples: List[SensorData] = []
    for i in range(n):
        ts = i * DT_NS
        samples.append(SensorData(
            timestamp=ts,
            accX=np.random.normal(0, ACC_NOISE * noise_scale * 0.5),
            accY=GRAVITY + np.random.normal(0, ACC_NOISE * noise_scale * 0.2),
            accZ=np.random.normal(0, ACC_NOISE * noise_scale * 0.5),
            gyroZ=np.random.normal(0, GYRO_NOISE * noise_scale),
            magX=MAG_STRENGTH * 0.0 + np.random.normal(0, MAG_NOISE * noise_scale),
            magY=MAG_STRENGTH * 1.0 + np.random.normal(0, MAG_NOISE * noise_scale),
        ))
    gt = [(0, 0)]
    return samples, gt


def simulate_parking_scenario(noise_scale: float = 1.0
                              ) -> Tuple[List[SensorData], List[Tuple[float, float]]]:
    """30m north -> left turn -> 15m west -> left turn -> 10m south."""
    step_length = 0.7
    segments = [
        (0.0, 30.0),            # north 30m
        (3 * math.pi / 2, 15.0),  # west (heading=270°) 15m
        (math.pi, 10.0),        # south 10m
    ]
    turn_dur = 1.5
    samples: List[SensorData] = []
    t0 = 0

    prev_h = None
    for h, dist in segments:
        if prev_h is not None:
            st = _turn_samples(prev_h, h, turn_dur, t0_ns=t0, noise_scale=noise_scale)
            samples.extend(st)
            t0 += len(st) * DT_NS
        dur = (dist / step_length) / WALK_FREQ
        sw = _walk_samples(dur, h, t0_ns=t0, noise_scale=noise_scale)
        samples.extend(sw)
        t0 += len(sw) * DT_NS
        prev_h = h

    gt = [(0, 0), (0, 30), (-15, 30), (-15, 20)]
    return samples, gt
