# Iron's Artifice 移植：对应位置未确定，本次不改文件

## 结论

不复制、不覆盖、不改写任何文件。`D:\Code\ModText\ExWorld\res\irons-artifice` 就是当前工作区里的 `res/irons-artifice`，不是另一份待迁入的版本。按你的要求，对应位置无法确定时先停下，不猜测后改动。

## 已核对

- 当前工作区根目录：`D:\Code\ModText\ExWorld`。ExWorld 是 Minecraft `1.21.1` / NeoForge `21.1.248`（`gradle.properties`）。
- 你给出的源路径与该目录是同一路径。源与“当前版本对应位置”文件数、总字节都是 1286 个文件、44,538,534 字节。
- 目录内已有：
  - `irons-artifice-26.1.2-1.0.0/` 与 `irons-artifice-main/`：各 642 个文件、12,919,881 字节，相对路径和大小一致，没有只存在于一侧的文件。
  - `irons-artifice-26.1.2-1.0.0.zip`、`irons-artifice-main.zip`。
- 这两份解压树的 `gradle.properties` 都是 Minecraft `26.1.2`、NeoForge `26.1.2.84`、`mod_id=irons_artifice`、许可为 `All Rights Reserved`。
- `D:\Code` 下没有另一份 ExWorld。当前 `src/`、`lotm/` 和构建脚本没有引用 `irons_artifice` / `irons-artifice`。`settings.gradle` 也没有把该目录接成子工程。
- `res/` 里另外只有 `AUI-snow`、`BOs-Easy-NPC-1.20.1` 的参考包，不能据此推断 Iron's Artifice 该落到哪。

## 冲突与建议

没有可安全覆盖的“另一处同名文件”。若把 26.1.2 源码猜进当前 1.21.1 的 `src/` 或新子工程，会改包名、构建和无关代码，也和“保持原文件名、相对路径、内容含义，且不改无关部分”冲突；许可是 All Rights Reserved，也不能并进 ExWorld 再重新授权。建议：在你指出真正的目标根目录之前，保持 `res/irons-artifice` 不动。

## 已移植 / 未移植

- 已移植：无。未执行复制。
- 未移植：`res/irons-artifice` 全部 1286 个文件。原因：源和当前工作区是同一目录，另一版本的对应位置未知。

## 需要你补充

请给下面其中一项，确认后再做对照，有差异先列出再覆盖：

1. 另一个版本的根目录绝对路径（当前打开的必须是那个版本；若还是 `D:\Code\ModText\ExWorld`，则没有可移植目标）。
2. 若目标就是当前仓库内的某个子目录，请给出该相对路径。不会默认改到 `src/`、`lotm/` 或新建子工程。

确认目标后的做法：只处理该目录内的文件和子目录；按原相对路径放到目标的对应位置；同名且内容相同则跳过；同名但内容不同则先列出差异和建议，不直接覆盖；目标外文件不改。完成后列出已移植、跳过和未移植及原因。