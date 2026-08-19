# Origins: Umbrellas — 当部の Forge 1.20.1 移植版

**上流は [Fusion-Flux/Origins-Umbrellas](https://github.com/Fusion-Flux/Origins-Umbrellas)（Fabric 専用・LGPL-3.0）。
当部が Forge 1.20.1 へ移した。挙動は変えていない。**

⚠ **免許の表示が2か所で食い違っている。** Modrinth の頁は `MIT` と表示するが、
上流リポジトリの `LICENSE` は **LGPL-3.0** の全文。**厳しいほうに合わせて LGPL-3.0 として扱う**
（`LICENSE` を同梱し、`mods.toml` の `license` も `LGPL-3.0-only`、ソースはこの公開リポジトリに置く）。

## 何をするMODか

傘をどちらかの手に持っている間、**天気に当たっていないことになる**。
Apoli がいくつもの能力をまさにその問いから決めているので、そこに効く。

| 問い | 傘を持っている間 |
| - | - |
| `apoli:exposed_to_sun`（日に晒されている） | **偽**。日光で焼ける種族が真昼の平地を渡れる |
| `apoli:exposed_to_sky`（空が見えている） | **偽** |
| `apoli:in_rain`（雨に打たれている） | **偽**。⚠ **バニラも同じ判定を読む**ので、雨で火が消えない・エンダーマンが雨で傷まない等も一緒に起きる |

傘は**壊れない。濡れる**。雨に当たっている間は10ティックごとに1、雨から出ると20ティックごとに乾く
（暖かいバイオームで1段、しまい込まずに手か左手に持っていればさらに1段速い）。
濡れきると乾くまで何も防がない。上限は1200。

作り方は**革4・棒2**（上流のレシピのまま）。染められる。

## 当部が変えたところ

**挙動は変えていない。Fabric でしか無い入口を Forge の入口に置き換えただけ。**

| 上流（Fabric） | 当部（Forge） |
| - | - |
| `ModInitializer` でアイテム登録 | `DeferredRegister<Item>` |
| `ClientModInitializer` の色付け | `RegisterColorHandlersEvent.Item` |
| `PlayerEntityMixin#tick`（濡れ・乾き） | `TickEvent.PlayerTickEvent`（**サーバ側だけ**。損傷値は枠の同期に乗る） |
| `ExposedToSunConditionMixin` ＋ `EntityConditionsMixin` の2本 | **`SimpleEntityConditionMixin` 1本** |
| `EntityMixin#isBeingRainedOn` | `EntityMixin#isInRain`（同じメソッドの Mojang 側の名前） |

⚠ **条件まわりが1本に減った理由。** 上流は Fabric 版 Apoli の作りに合わせて、
`exposed_to_sky` に届くためだけに「登録時に条件factoryを丸ごと包み直す」`@WrapOperation` を書いている。
EdwinMindcraft の Forge 版 Apoli は**両方を1クラスの public static 2本**に通しているので、そこに当てれば済む
（`apoli-forge-1.20.1-2.9.0.8.jar` の `ApoliEntityConditions` の `BootstrapMethods` 表を読んで確認:
31番 → `SimpleEntityCondition.isExposedToSun`／35番 → `同 isExposedToSky`／
32番 → `EntityAccessor.callIsBeingRainedOn`＝`@Invoker("isInRain")`）。

⚠ **`isActuallyRainedOn` はバニラの判定を書き写している。** `player.isInRain()` を呼ぶと
**このMOD自身の mixin が「雨に当たっていない」と答える**ので、傘が濡れも乾きもしなくなる。
上流は一時的な旗で mixin を迂回していたが、当部は判定をその場で書いた（1.20.1 の `Entity#isInRain` と同じ）。

⚠ **上流にあって当部に無いもの**: MixinExtras（`@ModifyReturnValue`）は使っていない。
素の `@Inject(at = @At("RETURN"), cancellable = true)` で足りるうえ、eruto-mc の他16本が
MixinExtras を1本も使っていないため、依存を増やさないほうを採った。

## ビルド

```bash
cd eruto-mc/origins_umbrellas
./gradlew build --no-daemon
```

⚠ **`libs/` は要らない。** 当てているのは apoli だけで、当部パッチ版の origins には触らない
（`shifting_origins` が `libs/origins.jar` を要求するのとはここが違う）。
apoli は cursemaven からファイルIDで取るので、**clone しただけでビルドできる**。

## 両側に置く

⚠ **アイテムを登録するので、クライアントとサーバの両方に置く。**
登録はログイン時に同期され、持っていない側は `Failed to load registry` で弾かれる。
