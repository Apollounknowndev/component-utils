# Component Utils

Currently, this mod adds two new features to help with working with item components: 
A `changed component(s)` tooltip and the `/component_utils` command.

## `changed component(s)` tooltip

If advanced tooltips are enabled, under the `x component(s)` tooltip line will be an `x changed component(s)` line.
This will show how many components are added/removed from the base component list for the item.

## `/component_utils`

The `/component_utils` command provides several utilities for working with item components.

The basic setup for all subcommands looks like this:

`/component_utils <target selector> <slot>`

- `<target selector>` is a target selector like `@s`. Like with `/data` and `/attribute`, it can't target multiple entities at once.
- `<slot>` is a slot name, such as `weapon.mainhand`. It does not work with slot ranges.


After that, there are four subcommands.

When relevant, following example commands will assume the target item is a sharpness 2 diamond sword renamed to "Cool Sword".

---

### `get`

Outputs the contents of component(s) on the item. If a specific components is not specified, the output will be formatted like the component map in the `/give` command.

**Syntax**

`/component_utils ... get <component entry type> [<component type>]`

- `<component entry type` determines where to try to get the component from. Possible values include:
  - `explicit`: Gets the component from the item stack's `components` field.
  - `implicit`: Gets the component from the item's default components.
  - `explicit`: Gets the component from the `components` field if present, otherwise fallbacks to the item's default components.
- `[<component type>]` is the ID for the component type. If omitted, the game will return *every* component.

**Examples**

`/component_utils ... explicit`
```
Item stack has the following components: [minecraft:enchantments={levels: {"minecraft:sharpness": 2}},minecraft:custom_name='"Cool Sword"'] [Copy]
```

`/component_utils ... explicit enchantments`
```
Component 'minecraft:enchantments' on item stack has the following contents: {levels: {"minecraft:sharpness": 2}} [Copy]
```

`/component_utils ... implicit rarity`
```
Component 'minecraft:rarity' on item stack has the following contents: "common" [Copy]
```

`/component_utils ... all bees`
```
Component 'minecraft:bees' is not explicitly nor implicitly present on item stack
```

---

### `merge`

Merges components onto the item.

**Syntax**

`/component_utils ... merge <components>`

- `<components>`: A map of component types to their values. This is in the exact same format as the item field in the `/give` command, just without an item specified.

**Example**

`/component_utils ... merge [rarity="rare"]`
```
Merged components into item stack
```

---

### `clear`

Clears component changes from the item.

**Syntax**

`/component_utils ... clear [<component type>]`

 `[<component type>]`: The ID for the component type. If omitted, the game will clear *every* changed component.

**Examples**

`/component_utils ... clear`
```
Cleared all component changes from item stack
```

`/component_utils ... clear enchantments`
```
Cleared changes to component 'minecraft:enchantments' from item stack
```

`/component_utils ... clear rarity`
```
Component 'minecraft:rarity' is not on item stack
```

---

### `remove`

Removes components from the item.

**Syntax**

`/component_utils ... remove <component type>`

`<component type>`: The ID for the component type. Cannot be omitted.

**Examples**

`/component_utils ... remove enchantments`
```
Cleared changes to component 'minecraft:enchantments' from item stack
```

`/component_utils ... remove rarity`
```
Removed implicit component 'minecraft:rarity' from item stack
```

`/component_utils ... remove bees`
```
Component 'minecraft:bees' is not present on item stack
```