# Vehicle and part player animations

Vehicles and parts may define an ordered `playerAnimations` list inside their
`rendering` section. These client-side animations are enabled by
the existing `playerTweaks` client setting and are visible in first and third
person. They only ever affect players, never other humanoid mobs.

Within each model, the first active entry for a limb wins. Put short actions,
such as reaching for a starter key, before persistent targets such as a
steering wheel grip. For seated players, vehicle entries take priority over
part entries. When two parts offer a target for the same seated limb, the
first eligible part in load order wins. For standing players, the nearest
eligible target across vehicles and parts wins.

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
| `seatSlot` | Seated only | — | Zero-based seat slot indices, such as `[6, 7, 8]`. Vehicle entries use seats mounted directly on the vehicle. Part entries use seats mounted directly on the defining part or one of its mounting parents. Omit for exterior entries and for an entry targeting the defining seat part's own rider. |
| `applyAfter` | Yes | — | `objectName` of an entry in the defining model's `rendering.animatedObjects` whose complete animated transform is applied to the target. |
| `pos` | Yes | — | Attachment coordinate in the defining model's local space. The exact center of the hand or foot end face reaches this point. |
| `limbScale` | No | `[1, 1, 1]` | Local X/Y/Z scale for the limb. Scaling remains anchored at the end-face center, so changing it does not move the attachment point. |
| `range` | Exterior only | — | Maximum distance in blocks from `pos` to the center of the player's torso. An exterior entry omits `seatSlot`, uses a positive `range`, and overrides only its targeted limb. |
| `activeAnimations` | No | Always active | Normal MTS animation list that controls whether this entry is eligible. |
| `duration` | No | `10` | Reach and release transition time in ticks. Use `0` for an immediate transition. After the reach, the target follows `applyAfter` directly without transition lag. |
| `lingerDuration` | No | `0` | Ticks for which the limb remains attached after a normal entry becomes inactive. For `oneShot`, this is the resting time after the reach completes. |
| `oneShot` | No | `false` | Triggers once on the inactive-to-active edge, reaches over `duration`, rests for `lingerDuration`, then releases even if the variable remains active. It re-arms after becoming inactive. |
| `triggerOnChange` | No | `false` | With `oneShot`, triggers on either edge rather than only activation. This lets one entry handle both states of a toggle, such as opening and closing a door. Initial loading is not a trigger. |
| `initiatingPlayerOnly` | No | `false` | Selects the entry only for the player whose authenticated client action caused the active state to change. Use it for shared controls where only the actual operator should reach. Persistent shared grips should leave this `false`. |
| `gripOverrides` | No | — | Ordered alternative targets within the entry. The first active override wins and replaces the parent values it defines. |

Use either a non-empty `seatSlot` or a positive `range`. On a seat part,
omit both to animate its own rider. Conditions use the defining vehicle or
part's variables and are evaluated once per tick, which is also the
resolution of one-shot and linger timing.

`initiatingPlayerOnly` attribution covers synchronized variable changes,
clickable actions, GUI interaction, and custom keypresses. Direct variables are
matched to their entity and variable. Short-lived derived values, including
variable-modifier outputs, `player_interacting`, and clutch-shift pulses, can
inherit a recent authenticated interaction. On parts, this fallback uses
interactions on the defining part. Direct references can also match a variable
from another entity in the same assembly through normal MTS variable lookup.
An automatic release retains the player who started the animation for its
configured `lingerDuration`.

## Example part entry

A steering wheel part mounted on a vehicle with its driver's seat in slot 4
can use this entry in its own `rendering.playerAnimations`. Define
`wheel` in the part's `rendering.animatedObjects` and measure `pos` from
the part model's origin.

```json
{
  "limb": "armright",
  "seatSlot": [4],
  "applyAfter": "wheel",
  "pos": [-0.12, 0.06, 0],
  "duration": 3
}
```

For a part placed in the world, use a positive `range` to let a standing
player reach its target.

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

On pack load and hotload, entries with conflicting or missing seat and range
selectors, invalid ranges, or unknown `applyAfter` names are logged and skipped.
This includes `applyAfter` on grip overrides. The rest of the vehicle or part
loads normally. Entries on other pack items are logged and cleared.

Schema errors are not covered by this and fail the whole vehicle, as they do for
any other field: a `limb` value that is not one of the four lowercase names, a
missing required field, or a `seatSlot` written as a bare number instead of an
array. A slot index with no eligible seat produces no pose.

## Rendering

The limb rotates from its vanilla shoulder or hip and stretches along its local
length to reach the target. Clothing parts inherit the transform.

In first person, the vanilla hand is replaced only for arms that an animation
controls (including during their release), so a held item in the other hand is
still drawn normally. The held item of an animated arm is not drawn. The
animated limbs are drawn in world space instead; the torso and head remain
hidden so they do not obstruct the cockpit view.
