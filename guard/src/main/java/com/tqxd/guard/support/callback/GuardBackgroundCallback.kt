package com.tqxd.guard.support.callback

/**
 * 前后台切换回调
 */
interface GuardBackgroundCallback {
    /**
     * 前后台切换回调
     * @param background Boolean
     */
    fun onBackground(background: Boolean)
}