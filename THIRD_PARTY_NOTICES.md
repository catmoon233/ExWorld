# Third-party notices

- The advanced camera behavior incorporates code adapted from `StarRailExpress2`, licensed under GNU GPL v3. Its original copyright and license headers are retained in adapted source files.
- Iron's Spells 'n Spellbooks is a required runtime dependency. Its code and assets, including spell icons, remain subject to that project's license; ExWorld references those assets by resource identifier and does not relicense them.
- Minecraft, NeoForge, GeckoLib, Player Animator, Curios, Sodium and other runtime dependencies remain subject to their respective licenses.
- Item tooltip chrome was designed against Simply Tooltips, EnhancedTooltips, ColorTooltips and Obscure Tooltips as visual references only. ExWorld ships original tooltip code and does not bundle or relicense those projects.
- Player backpack grid occupancy, stacking, rotation and creative size/quality editing were designed against PetiteInventory and Item-Rarity as visual/interaction references only. ExWorld ships original inventory code and does not bundle or relicense those AGPL-3.0 projects.

- WebView2 uses the JNA 5.14.0 already shipped with Minecraft (Apache-2.0 or LGPL-2.1). ExWorld does not bundle a second copy.
- `WebView2Loader.dll` is Microsoft's redistributable WebView2 bootstrap loader, extracted at build time from the official `Microsoft.Web.WebView2` NuGet package and loaded only when the Windows client opens the window. It is not linked into the GPL sources. The Edge WebView2 Runtime itself is not bundled.

- The WebView page and NPC screens follow the color, bevel and panel language of ApricityUI's Ore theme in `res/AUI-snow`. That theme is adapted from Minecraft-CSS under MPL-2.0. ExWorld ships original compact drawing and does not bundle the Ore font files or the full theme.

- `irons_artifice/` is a local 1.21.1 NeoForge port of Iron's Arms 'n Artifice. That code and its assets remain All Rights Reserved; ExWorld does not relicense or claim the right to redistribute them.
