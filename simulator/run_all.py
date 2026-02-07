#!/usr/bin/env python3
"""
Run all PDR simulation scenarios and produce reports + plots.
Usage:  python run_all.py
"""

import sys, os, math
sys.path.insert(0, os.path.dirname(__file__))

from pdr_engine import PDREngine, SensorData
from path_simulator import (
    simulate_straight, simulate_l_shape, simulate_rectangle,
    simulate_stationary, simulate_parking_scenario,
)
from validator import validate, ValidationResult
from visualizer import plot_trajectory


def run_scenario(name, samples, gt, total_dist, threshold, metric="endpoint_error"):
    engine = PDREngine()
    for s in samples:
        engine.on_sensor_update(s)
    result = validate(name, engine.trajectory, gt, engine.step_detector.step_count,
                      total_dist, threshold, metric)
    plot_trajectory(name, engine.trajectory, gt, result.endpoint_error)
    return result


def main():
    results = []

    # 1. Straight 50m north
    s, gt = simulate_straight(50, 0.0)
    results.append(run_scenario("Straight 50m North", s, gt, 50, 5.0))

    # 2. Straight 30m east
    s, gt = simulate_straight(30, math.pi / 2)
    results.append(run_scenario("Straight 30m East", s, gt, 30, 3.0))

    # 3. L-shape 30+20
    s, gt = simulate_l_shape(30, 20)
    results.append(run_scenario("L-shape 30+20", s, gt, 50, 5.0))

    # 4. Rectangle 20x10
    s, gt = simulate_rectangle(20, 10)
    results.append(run_scenario("Rectangle 20x10", s, gt, 60, 6.0, "closure_error"))

    # 5. Stationary 60s
    s, gt = simulate_stationary(60)
    results.append(run_scenario("Stationary 60s", s, gt, 0, 0.5, "drift"))

    # 6. Parking scenario
    s, gt = simulate_parking_scenario()
    results.append(run_scenario("Parking Scenario", s, gt, 55, 5.5))

    # 7. High noise rectangle
    s, gt = simulate_rectangle(20, 10, noise_scale=2.0)
    results.append(run_scenario("Rectangle High Noise", s, gt, 60, 9.0, "closure_error"))

    # Print reports
    print("\n" + "=" * 60)
    print("PDR SIMULATOR — RESULTS SUMMARY")
    print("=" * 60)
    passed = 0
    for r in results:
        print()
        print(r.report())
        if r.passed:
            passed += 1

    print("\n" + "=" * 60)
    print(f"TOTAL: {passed}/{len(results)} scenarios passed")
    print("=" * 60)

    if passed < len(results):
        print("\n⚠️  Some scenarios failed — see details above.")
    else:
        print("\n🎉 All scenarios passed!")

    print(f"\nPlots saved to: {os.path.join(os.path.dirname(__file__), 'results')}/")


if __name__ == "__main__":
    main()
