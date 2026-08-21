# Behavior Validation

Inspired by GeeTest: the frontend collects the full interaction trajectory and submits it with the answer; the backend checks whether the behavior looks human and matches the answer.

## td Payload

```text
m|w|h|s|e|p
```

| Field | Description |
| --- | --- |
| `m` | Protocol version (currently `1`) |
| `w` / `h` | Viewport (container) size |
| `s` / `e` | Start / end timestamp (ms) |
| `p` | Points: `timeMs,x,y,type;...` |

Coordinates are normalized 0~1. Event types:

| Code | Meaning |
| --- | --- |
| `0` | START |
| `1` | MOVE |
| `2` | UP |
| `3` | DOWN |

Example:

```text
1|300.6|262.0|1787290676714|1787290683694|0,0.0,0.5,0;21,0.1,0.5,1;...
```

## Compression

The frontend compresses `td` with gzip + base64url (`H4sI...`). The backend auto-detects compressed and plain formats.

## Architecture

`AbstractBehaviorValidator` is the unified template:

1. Parse the `td` payload
2. Common rules: protocol, viewport, duration, point count, time order, coordinate range, jump distance
3. Delegate to subclasses for event sequences
4. Delegate to subclasses for answer correlation

| Validator | Checks |
| --- | --- |
| `SliderBehaviorValidator` | START → MOVE → UP; final x matches `xNorm` |
| `ClickBehaviorValidator` | DOWN/UP pairs, count, order, coordinates, click duration |
| `RotateBehaviorValidator` | Drag event sequence (angle checked by the generator) |
| `CurveBehaviorValidator` | START → MOVE → UP, no clicks; trace ends match the answer curve ends |
| `SlideCurveBehaviorValidator` | START → MOVE → UP; final x matches the curve swing `xNorm` |

The curve generator also checks geometry: the drawn start/end must land near the
guide curve markers, and the coverage of the expected curve must reach the
threshold (default 60%).

## Risk Scoring (Second Layer)

Hard rules only catch obviously fake trajectories. To detect bots that fake
Bezier curves and add jitter, the engine adds a second layer of statistical
scoring: `DragBehaviorRiskScorer` / `ClickBehaviorRiskScorer` combine weak
signals into a normalized 0~1 score and only reject when the score exceeds the
profile threshold, reducing false positives.

Drag (slider / rotate / curve / slide-curve) features:

| Feature | Meaning | Bot signature |
| --- | --- | --- |
| `speed-uniformity` | Coefficient of variation of move speeds | Constant-speed straight line |
| `end-deceleration` | End speed / peak speed | No deceleration before release |
| `start-pause` | Pause between press and first movement | Moving immediately after press |
| `path-efficiency` | Path length / straight-line distance | Perfectly straight path |

Click features:

| Feature | Meaning | Bot signature |
| --- | --- | --- |
| `move-uniformity` | CV of move speeds between clicks | Constant-speed movement |
| `dwell-uniformity` | CV of press durations | Identical click durations |
| `interval-uniformity` | CV of click intervals | Perfectly regular rhythm |
| `duplicate-downs` | Bit-identical repeated click coordinates | Exact same click position |

Features with insufficient samples are automatically excluded, so sparse H5 /
mini-program traces are not penalized. Touch profiles also default to a looser
threshold (`0.8`; web uses `0.65`).

Enable it with:

```yaml
captcha:
  behavior:
    enabled: true
    risk-enabled: true
    risk-threshold: 0.65
```

Scores above the threshold return the `BEHAVIOR` code as well.

## Per-Client Profiles

| Profile | Scenario | Characteristics |
| --- | --- | --- |
| Default (web) | PC mouse | Dense sampling, strict jump threshold |
| `h5` | Mobile browser / WebView | Sparse touch sampling, looser jump |
| `mini_program` | WeChat mini program | Same as touch |

Default thresholds:

| Setting | Web | H5 / mini program |
| --- | --- | --- |
| `max-jump-ratio` | `0.5` | `0.9` |
| `point-tolerance` | `0.05` | `0.08` |
| `min-click-duration-ms` | `30` | `20` |

## Configuration

```yaml
captcha:
  behavior:
    enabled: true
    h5:
      max-jump-ratio: 0.9
      point-tolerance: 0.08
      min-click-duration-ms: 20
    mini-program:
      max-jump-ratio: 0.9
```

When enabled, missing or invalid `td` returns the `BEHAVIOR` code.
