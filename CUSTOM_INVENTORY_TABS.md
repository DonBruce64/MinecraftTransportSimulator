# Pack-defined creative inventory side tabs

Packs may split their normal Immersive Vehicles creative tab into ordered side tabs. The feature is available on every supported adapter from Minecraft 1.12.2 through 1.21.1.

Add `customInventoryTabs` to `packdefinition.json`. Array order is display order from top to bottom:

```json
{
  "packID": "examplepack",
  "packName": "Example Pack",
  "customInventoryTabs": [
    {
      "name": "Aircraft",
      "icon": "gui/creative/aircraft"
    },
    {
      "name": "Ground vehicles",
      "itemIcon": "example_car"
    },
    {
      "name": "Shared content",
      "itemIcon": "otherpack:example_block"
    }
  ]
}
```

Each tab needs a unique `name` and either `icon` or `itemIcon`. Pack-relative texture icons resolve below `assets/<packID>/textures/`; the `textures/` prefix and `.png` suffix are optional. Texture icons should be square, preferably 16 by 16 pixels. `itemIcon` accepts a pack item `systemName` (or `packID:systemName`) and renders that item's normal model and texture, including 3D block-style models. When both fields are present, `itemIcon` takes priority if it resolves successfully.

Assign an item by adding `customTabGroups` to its `general` section. It may be one name or an array of names:

```json
{
  "general": {
    "name": "Example aircraft",
    "customTabGroups": ["Aircraft", "Shared content", "default"],
    "materialLists": []
  }
}
```

Names are exact and case-sensitive. An item appears in every valid group listed. The special name `default` also shows it in the normal pack tab. For definition-specific control, put `customTabGroups` in an entry's specific `definitions`/sub-definition object; that value overrides `general.customTabGroups` for that variant, just like definition-level creative visibility settings.

An item remains in the normal default pack tab when `customTabGroups` is absent or blank, when none of its names match a declared tab, or when its only matching name is duplicated in the pack's tab list. Selecting the pack's normal creative tab returns to the default group. When more than four side tabs exist, arrow buttons scroll through them and use the highlighted arrow sprite on hover.

Players can disable the feature with `renderingSettings.customInventoryTabs` in `mtsconfigclient.json`. Its default is `true`; when false, side tabs are hidden and all items appear together in the original pack tab.
