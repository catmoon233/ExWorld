package com.wan.gmmod.content.entities;

/**
 * 灵体标记接口。
 * <p>
 * 实现该接口的实体属于「灵体」——平时（未开启灵视时）对玩家<b>不可见</b>，
 * 只有当本地玩家开启了灵视（{@code ModAttachments.SPIRIT_VISION}）后，
 * 对应的渲染器才会渲染它们。
 * <p>
 * 可见性控制由各自的实体渲染器在 {@code shouldRender} 中读取本地玩家的
 * 灵视状态实现，不改变实体在服务端的碰撞 / tick 行为——即灵体依旧真实存在，
 * 只是「看不见」。
 */
public interface SpiritBeing {
}
