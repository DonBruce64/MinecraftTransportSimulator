# Dynamic Lights

Added dynamic lights. A light's blendable component can now light the world on its own: blocks, mobs and vehicles in its cone get lit, it casts shadows, and it draws a volumetric beam in the air and a corona on the lens. It works on every version: 1.12.2, 1.16.5, 1.18.2, 1.19.2, 1.20.1 and 1.21.1.

Nothing changes for existing packs. A component is only dynamic if the pack says so. Everything else renders like it always has.

## What Changed

- Added `isDynamicLight` and `lightPreset` to light `blendableComponents`, plus fields to tune the light. All fields are listed below.
- Added `light` to particles. Muzzle flashes, sparks, fire and the like can now light the area around them. The light fades and changes color with the particle.
- Dynamic lights work on every definable with a model: vehicles, parts, decors, poles and bullets.
- Shadows come from blocks and from models: vehicles, mobs, players, boats, minecarts and dropped items. Slabs, stairs, fences, doors and the like shadow with their real shape. Glass, grilles, invisible parts and other cut-out textures let light through their clear parts.
- Lamps rank by distance and strength. Headlights rank first, particles last. Only the strongest lights the client allows are drawn.
- Shader packs (Iris, Oculus, OptiFine): dynamic lights turn off and lamps draw their regular beams and flares. With `DynL_ShaderFB` on, lamps instead raise block light and draw a simple cone and flare.
- Old GPUs: if the context can't run the shaders, lamps use the same fallback. On 1.12.2 and 1.16.5 the shaders also run on the GL 2.1 context macOS gives.

## For Players

Client config (`mtsconfigclient.json`, also in the in-game config screen):

- `dynamicLights`: true by default. False draws the regular beams and flares.
- `DynL_shadows`: true by default. Turn off to save GPU time.
- `DynL_MaxLights`: 48 by default, 0 to 256. How many lights, beams and coronas are drawn at once. Lower is faster.
- `DynL_ShaderFB`: false by default. Set to true to keep lamps lit under a shader pack (simple cone and flare).

## For Pack Makers

Mark the real lamps only: headlights, tail, brake, reverse and turn signal lamps, landing and taxi lights. Don't mark cockpit or indicator LEDs. The mod never converts lights on its own.

The simplest dynamic headlight is a preset:

```json
"blendableComponents": [
  {
    "pos": [0.7, 0.85, 2.3],
    "axis": [0.0, -0.08, 1.0],
    "flareWidth": 0.5,
    "flareHeight": 0.5,
    "lightPreset": "headlight_low"
  }
]
```

Or set it up yourself:

```json
{
  "pos": [0.7, 0.85, 2.3],
  "axis": [0.0, -0.08, 1.0],
  "beamDiameter": 24.0,
  "beamLength": 32.0,
  "beamSoftness": 0.4,
  "flareWidth": 0.5,
  "flareHeight": 0.5,
  "isDynamicLight": true
}
```

The light is still turned on and off by the light's own `brightnessAnimations`. With dynamic lights off, the same component draws its regular beam and flare.

### Fields

All fields go on a `blendableComponents` entry. All are optional.

- `isDynamicLight`: makes the component a dynamic light.
- `lightPreset`: also makes it a dynamic light, with preset values. The fields below override what the preset sets. Switch presets with `brightnessAnimations` on your own high beam variable.
  - `headlight_low`: intensity 1.4, reach 28, 25 degree cone.
  - `headlight_high`: intensity 1.8, reach 60, 25 degree cone.
  - `brake_light`: intensity 1.0, reach 4, 150 degree cone. For stop, tail, signal and marker lamps.
  - `reverse_light`: intensity 0.8, reach 6, 120 degree cone.
- `pos`, `axis`: where the light comes from and where it points. No axis, or a `beamDiameter` of 0, makes a point light that shines in all directions.
- `beamDiameter`, `beamLength`: the cone. `beamLength` is how far the light reaches, and `beamDiameter` is the size of the spot at that distance.
- `lightAngle`, `lightReach`: the cone angle in degrees and the reach in blocks, set directly. Use these to keep a different regular beam for players with dynamic lights off.
- `beamSoftness`: 0 to 1, how much of the cone's outer edge fades out. 0 or unset means 0.4; for a hard edge use a small value such as 0.01.
- `beamIntensity`: how bright the light is. 1.4 is a dipped headlight, 1.8 a main beam, 2 to 4 a strong spotlight. Also works on regular components.
- `color`: color of this component only. Also works on regular components.
- `brightnessAnimations`: brightness of this component only, multiplied with the light's. Use it to blink one lamp while the others stay on. Also works on regular components.
- `flareWidth`, `flareHeight`: above 0 draws a corona at the lamp.
- `bloomIntensity`: corona size and strength, 0 to 999. 0 or unset means 1, a negative value draws no bloom.
- `lightBeamLength`: how much of the reach shows as a beam in the air, 0 to 1. 0 or unset means 0.45 of the reach, at most 24 blocks; a negative value shows no beam. This doesn't change what gets lit.
- `shadowStartDistance`: geometry closer to the lamp than this casts no shadow from it. Use it for lamps placed behind a lens, a grille or a housing. Default and minimum 0.35 blocks.
- `hideWhenDynLightActive`: hides this flare or beam while dynamic lights are on or a shader pack is active. Use it for stand-in flares and beams meant only for players without dynamic lights.

Particles take the same fields under `light`. `pos` is relative to the particle, and an `axis` makes it a spot. The color defaults to the particle's color and the brightness follows its transparency. Sub-particles don't inherit it.

Embeddium (1.20.1) and Sodium (1.21.1) are compile-only, for an optional mixin that keeps their chunk lighting in step. They are never shipped or required.
