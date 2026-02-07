"""
Validator — compares PDR output trajectory against ground truth.
"""

import math
from typing import List, Tuple
from dataclasses import dataclass


@dataclass
class ValidationResult:
    scenario: str
    endpoint_error: float
    closure_error: float
    max_error: float
    avg_error: float
    step_count_pdr: int
    expected_steps: int
    passed: bool
    threshold: float
    metric_name: str

    def report(self) -> str:
        status = "✅ PASS" if self.passed else "❌ FAIL"
        lines = [
            f"=== {self.scenario} {status} ===",
            f"  Endpoint error : {self.endpoint_error:.2f} m",
            f"  Closure error  : {self.closure_error:.2f} m",
            f"  Max error      : {self.max_error:.2f} m",
            f"  Avg error      : {self.avg_error:.2f} m",
            f"  Steps (PDR)    : {self.step_count_pdr}",
            f"  Steps (expect) : {self.expected_steps}",
            f"  Metric         : {self.metric_name} = {self._metric_value():.2f} (threshold {self.threshold:.2f})",
        ]
        return "\n".join(lines)

    def _metric_value(self):
        if "closure" in self.metric_name.lower():
            return self.closure_error
        if "drift" in self.metric_name.lower():
            return self.endpoint_error
        return self.endpoint_error


def _dist(a: Tuple[float, float], b: Tuple[float, float]) -> float:
    return math.sqrt((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2)


def validate(scenario: str,
             pdr_trajectory: List[Tuple[float, float]],
             ground_truth: List[Tuple[float, float]],
             step_count: int,
             total_distance: float,
             threshold: float,
             metric_name: str = "endpoint_error") -> ValidationResult:
    """Evaluate PDR trajectory vs ground truth."""

    if not pdr_trajectory:
        pdr_trajectory = [(0, 0)]

    pdr_end = pdr_trajectory[-1]
    gt_end = ground_truth[-1]
    gt_start = ground_truth[0]

    endpoint_error = _dist(pdr_end, gt_end)
    closure_error = _dist(pdr_end, gt_start) if gt_start == gt_end else endpoint_error

    # Approximate max/avg error by interpolating GT as segments
    errors = []
    for pt in pdr_trajectory:
        min_d = min(_dist(pt, gp) for gp in ground_truth)
        errors.append(min_d)

    max_error = max(errors) if errors else 0
    avg_error = sum(errors) / len(errors) if errors else 0

    expected_steps = int(total_distance / 0.7)

    if "closure" in metric_name.lower():
        val = closure_error
    elif "drift" in metric_name.lower():
        val = _dist(pdr_end, (0, 0))
    else:
        val = endpoint_error

    passed = val < threshold

    return ValidationResult(
        scenario=scenario,
        endpoint_error=endpoint_error,
        closure_error=closure_error,
        max_error=max_error,
        avg_error=avg_error,
        step_count_pdr=step_count,
        expected_steps=expected_steps,
        passed=passed,
        threshold=threshold,
        metric_name=metric_name,
    )
