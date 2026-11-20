# Vehicle player animations

Vehicles (and only vehicles) may define an ordered `playerAnimations` list
inside their `rendering` section. These client-side animations are enabled by
the existing `playerTweaks` client setting and are visible in first and third
person. They only ever affect players, never other humanoid mobs.

The first active entry for a limb wins. Put short, high-priority actions, such
as reaching for a starter key, before persistent targets such as a steering
wheel grip.

```json
"rendering": {
  "playerAnimations": [
    {
      "limb": "armright",
      "seatSlot": [4, 5],
      "initiatingPlayerOnly": true,
      "applyAfter": "starter_key",
      "pos": [-0.42, 1.08, 0.51],
      "limbScale": [0.75, 1.0, 0.75],
      "duration": 4,
      "lingerDuration": 6,
      "oneShot": true,
      "triggerOnChange": true,
      "activeAnimations": [
        {
          "animationType": "visibility",
          "variable": "engine_starter_1",
          "clampMin": 1,
          "clampMax": 1
        }
      ]
    },
    {
      "limb": "armright",
      "seatSlot": [4],
      "applyAfter": "steering_wheel",
      "pos": [-0.22, 1.12, 0.63],
      "duration": 0,
      "gripOverrides": [
        {
          "pos": [0.18, 1.16, 0.63],
          "duration": 3,
          "activeAnimations": [
            {
              "animationType": "visibility",
              "variable": "rudder",
              "clampMin": 20,
              "clampMax": 45
            }
          ]
        }
      ]
    }
  ]
}
```

## Entry parameters

| Parameter | Required | Default | Function |
| --- | --- | --- | --- |
| `limb` | Yes | — | Limb to control: `armleft`, `armright`, `legleft`, or `legright`. |
| `seatSlot` | Seated only | — | Array of zero-based indices into the vehicle's `parts` list, such as `[6, 7, 8]`, naming the seats this entry applies to. Only seats mounted directly on the vehicle match; a seat on another part never does. Omit it for an exterior animation. |
| `applyAfter` | Yes | — | `objectName` of the `rendering.animatedObjects` entry whose complete animated transform is applied to the target. |
| `pos` | Yes | — | Attachment coordinate in vehicle model space. The exact center of the hand or foot end face reaches this point. |
| `limbScale` | No | `[1, 1, 1]` | Local X/Y/Z scale for the limb. Scaling remains anchored at the end-face center, so changing it does not move the attachment point. |
| `range` | Exterior only | — | Maximum distance in blocks from `pos` to the center of the player's torso. An exterior entry omits `seatSlot`, uses a positive `range`, and overrides only its targeted limb. |
| `activeAnimations` | No | Always active | Normal MTS animation list that controls whether this entry is eligible. |
| `duration` | No | `10` | Reach and release transition time in ticks. Use `0` for an immediate transition. After the reach, the target follows `applyAfter` directly without transition lag. |
| `lingerDuration` | No | `0` | Ticks for which the limb remains attached after a normal entry becomes inactive. For `oneShot`, this is the resting time after the reach completes. |
| `oneShot` | No | `false` | Triggers once on the inactive-to-active edge, reaches over `duration`, rests for `lingerDuration`, then releases even if the variable remains active. It re-arms after becoming inactive. |
| `triggerOnChange` | No | `false` | With `oneShot`, triggers on either edge rather than only activation. This lets one entry handle both states of a toggle, such as opening and closing a door. Initial loading is not a trigger. |
| `initiatingPlayerOnly` | No | `false` | Selects the entry only for the player whose authenticated client action caused the active state to change. Use it for shared controls where only the actual operator should reach. Persistent shared grips should leave this `false`. |
| `gripOverrides` | No | — | Ordered alternative targets within the entry. The first active override wins and replaces the parent values it defines. |

Each entry must have exactly one of a non-empty `seatSlot` or a `range` above 0.
`activeAnimations` are evaluated once per vehicle tick, which is also the
resolution of one-shot and linger timing.

`initiatingPlayerOnly` attribution covers synchronized variable changes,
clickable actions, GUI interaction, and custom keypresses. Direct variables are
matched exactly. Short-lived derived values, including variable-modifier
outputs, `player_interacting`, and clutch-shift pulses, inherit the recent
authenticated interaction on the same vehicle. Automatic and server-originated
changes have no initiating player. Once an actor-only entry starts, an automatic
release retains that actor for its configured `lingerDuration`.

## Grip override parameters

| Parameter | Required | Default | Function |
| --- | --- | --- | --- |
| `pos` | Yes | — | Replacement attachment coordinate. |
| `applyAfter` | No | Parent value | Replacement animated-object name. |
| `limbScale` | No | Parent value | Replacement local X/Y/Z limb scale. |
| `activeAnimations` | No | Always active | Normal MTS animation list controlling this override. Overrides are checked in list order. |
| `duration` | No | Parent value | Transition time for changing to or from this grip. |
| `lingerDuration` | No | Parent value | Linger or one-shot resting time for this grip. |
| `oneShot` | No | Parent value | Enables one-shot behavior for this grip. |
| `triggerOnChange` | No | Parent value | Makes this one-shot grip trigger on either state change. |

## Load-time validation

When the pack loads, each entry is checked and an invalid one is logged (with
the pack ID, system name, and entry number) and dropped, while the rest of the
vehicle loads normally. An entry is invalid if it has both or neither of
`seatSlot` and `range`, or if its `applyAfter` (or the `applyAfter` of a grip
override) is not an `objectName` in `rendering.animatedObjects`. Defining
`rendering.playerAnimations` on anything other than a vehicle is logged as an
error and the list is cleared.

Schema errors are not covered by this and fail the whole vehicle, as they do for
any other field: a `limb` value that is not one of the four lowercase names, a
missing required field, or a `seatSlot` written as a bare number instead of an
array. A `seatSlot` index that is not a seat mounted directly on the vehicle is
not an error; the entry simply never matches.

## Rendering

The limb rotates from its vanilla shoulder or hip and stretches along its local
length to reach the target. Clothing parts inherit the transform.

In first person, the vanilla hand is replaced only for arms that an animation
controls (including during their release), so a held item in the other hand is
still drawn normally. The held item of an animated arm is not drawn. The
animated limbs are drawn in world space instead; the torso and head remain
hidden so they do not obstruct the cockpit view.
