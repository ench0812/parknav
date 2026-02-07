"""
Visualizer — matplotlib plots for PDR vs ground truth.
"""

import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from typing import List, Tuple

RESULTS_DIR = os.path.join(os.path.dirname(__file__), "results")
os.makedirs(RESULTS_DIR, exist_ok=True)


def plot_trajectory(scenario: str,
                    pdr_traj: List[Tuple[float, float]],
                    gt_points: List[Tuple[float, float]],
                    endpoint_error: float,
                    filename: str | None = None):
    fig, ax = plt.subplots(1, 1, figsize=(8, 8))

    # Ground truth
    gx = [p[0] for p in gt_points]
    gy = [p[1] for p in gt_points]
    ax.plot(gx, gy, "g-o", linewidth=2, markersize=6, label="Ground Truth")

    # PDR trajectory
    px = [p[0] for p in pdr_traj]
    py = [p[1] for p in pdr_traj]
    ax.plot(px, py, "r-", linewidth=1, alpha=0.8, label="PDR Estimate")

    # Markers
    ax.plot(px[0], py[0], "bs", markersize=10, label="Start")
    ax.plot(px[-1], py[-1], "r^", markersize=10, label="PDR End")
    ax.plot(gx[-1], gy[-1], "gD", markersize=10, label="GT End")

    ax.set_title(f"{scenario}\nEndpoint Error: {endpoint_error:.2f} m")
    ax.set_xlabel("X (East) [m]")
    ax.set_ylabel("Y (North) [m]")
    ax.legend()
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.3)

    fname = filename or scenario.replace(" ", "_").replace("/", "_") + ".png"
    path = os.path.join(RESULTS_DIR, fname)
    fig.savefig(path, dpi=100, bbox_inches="tight")
    plt.close(fig)
    return path
