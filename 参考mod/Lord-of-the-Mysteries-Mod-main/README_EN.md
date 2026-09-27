# Lord of the Mysteries — Guimimod

> A large-scale RPG mod for Minecraft NeoForge, based on the novel *Lord of the Mysteries*.
> Sequence potions · Pathway roleplay · Spirituality divination · Marionette control · Ancient mysterious items — begin your Beyonder journey in the world of blocks now.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/) [![NeoForge](https://img.shields.io/badge/NeoForge-21.1.233-orange)](https://neoforged.net/) [![Java](https://img.shields.io/badge/Java-21-red)](https://adoptium.net/) [![GeckoLib](https://img.shields.io/badge/GeckoLib-4.9.1-blue)](https://www.geckolib.com/)

> 🌐 Language / 语言: [简体中文](README.md) | English

---

## ✨ About the Mod

You are a fresh "Sequence 9" newcomer who has just stepped into the Beyonder world. Gather materials, study recipes, brew your own potions before the cauldron, and climb the ladder of 22 pathways — but remember: **"At the same time as diving into the role and giving it your all, step back emotionally, look at things calmly, and through small comparisons, understand yourself and find your truest self."**.

> Feel free to try out the mod's content — many features are still missing their designs and acquisition methods, so it is **not recommended for survival play** yet.

- **22 Pathways × 10 Sequences**: Fool, Door, Error, Paragon, Hanged Man, Sun, Tyrant, White Tower, Visionary, Death, Darkness, Giant, Red Priest, Hermit, Moon, Mother, Abyss, Chained, Witch, Justiciar, Black Emperor, Wheel of Fortune — all implemented, sequences 9→0 for each pathway. (All low & mid sequences of every pathway are complete; high sequences are still in development.)
- **Roleplay-based advancement**: drinking the potion is only the beginning — advancement requires Acting Progress and Spirituality reserves, and higher sequences demand special conditions (such as performing the ritual amid a mermaid's song).
- **The price of losing control**: Sanity (SAN) and Corruption threaten you in real time; depleted Spirituality invites hallucinations and whispers.

## 🧪 Core Gameplay

### Potions & Recipe Scrolls
- All sequence potions are brewed in a **cauldron**: pour in purified water → toss in the ingredients → stir with a stick ×3.
- Every potion corresponds to a **recipe scroll** (shared scroll texture, distinguished by name and ingredient list). You must right-click to study it before brewing — scrolls come from ruin chests, quest rewards, and NPC trades.
- Material sources: killing creatures (every mob has its own drops), gathering flowers and herbs, **syringe blood drawing**, fishing, and mining.

### Pathway Abilities
- Advancement unlocks abilities: the Seer's pendulum intuition, the Clown's flying cards, the Magician's paper substitute, the Faceless One's disguises, the Marionettist's spirit threads...
- **Skill bar** (3 pages × 5 slots) with free configuration — scroll through pages, trigger with number keys.
- Advanced features such as Card Scatter (Fool · Seq 8) and the Disguise Library (Fool · Seq 6).

### Spirituality & the Mystical
- **Spirit Vision**: see the spirit world invisible to mortals.
- **Divination trio**: pendulum divination, mirror divination, spirit communing — plus anti-divination interference fields.
- **Marionettist**: control creatures with spirit threads and command your marionettes in battle through shared vision.
- **Distortion** (Black Emperor · Seq 6): rule-level powers that rewrite the rules of an area.

### Ancient Mysterious Items
The "ancient items with a trace of mystical power" series — a Broken Icon Finger, an Asylum Admission Record, a Scorched Robe Fragment, a Bloodstained Sixpence, and more. Each hides both power and a price. (More items are being added.)

## 🌍 World (creature types & world generation are still being updated)

- **30+ magical flowers and herbs** spawn across the Overworld in cross-shaped models — the foundation of potion brewing. (More varieties are being added.)
- **Lucky Garden**: a Lucky Flower stands atop Lucky Flower Soil, surrounded by four-leaf clovers — with a small chance of hiding a silver one.
- **40+ creatures**: wraiths, werewolves, mermaids, nuns and priests, death ravens, the dual-form Nightmare Evil Eye, the silver war bear... each with its own material drops. (More creatures are being added.)
- **City system**: structures and village overhauls, recreating a city with urban gameplay. (In development.)
- **Special weather systems** — haze, blood moon. (Being added.)
- **22-pathway main quests, side quests, special quests and timed quests**. (Being added.)
- **Animated pendulums and canes** enrich the play experience.

## 🎮 Quick Start

1. Search the grasslands for flowers and herbs such as **Gold Mint / Night Fragrance / Dragon Blood Grass / Poison Hemlock**, and kill creatures to collect materials.
2. Craft **Purified Water** and a **Cauldron**, right-click the cauldron with the water, toss in the ingredients, then stir with a stick ×3.
3. Drink the **Sequence 9 potion** to take your position! Then press `J` to open the quest journal and follow the story.
4. Press `K` to configure your skill bar and use `1~5` to trigger abilities.

### Admin Commands (OP)

```
/guimi max [player]        # Max out Spirituality and Acting Progress
/guimi demote [player]     # Strip a player back to mundane
/guimi quest force <task-id> / complete <task-id>
```

## 📦 Installation

1. Install [Minecraft 1.21.1](https://www.minecraft.net/) and [NeoForge 21.1.233](https://neoforged.net/).
2. Drop `guimi_mod-0.0.1.jar` into your `mods` folder (you also need [GeckoLib 4.9.1](https://www.curseforge.com/minecraft/mc-mods/geckolib)).
3. Launch the game and begin your journey.

## 🛠️ Development

```bash
git clone https://github.com/1937914339/guimi.git
cd guimi
./gradlew runClient    # Launch the dev client
./gradlew build        # Build output goes to build/libs/
```

## 📄 License

This project's source code is open-sourced under the [MIT License](LICENSE).
Code, textures and models may be used free of charge — **commercial use is prohibited**.
When reposting or making derivative works, please keep the attribution and license notice at the top of this file.

## ⚠️ Disclaimer

1. This mod is a **fan-made derivative work** and has **no affiliation or partnership** with *Lord of the Mysteries* author Cuttlefish That Loves Diving, China Literature Group, or Minecraft official (Mojang/Microsoft).
2. This mod is provided **"as is"**, without warranty of any kind, express or implied. Any problems arising from its use (including but not limited to save corruption, game crashes, or data loss) are borne at the user's own risk.
3. Using this mod or modified versions of it for **commercial purposes** is strictly prohibited, as is unauthorized republishing of its content on other platforms for profit.
4. Please obtain the mod only from the official Releases or build artifacts of this repository — the safety of versions distributed through third-party channels cannot be guaranteed.

## 🙏 Acknowledgements

- [Cuttlefish That Loves Diving](https://book.qidian.com/info/1010868264) — author of *Lord of the Mysteries*
- [NeoForge](https://neoforged.net/) / [GeckoLib](https://www.geckolib.com/) — technical foundations
- All players who helped with testing and feedback
