package demo.atomofiron.insets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import demo.atomofiron.insets.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import demo.atomofiron.insets.fragment.map.PlayerFragment
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.invoke
import lib.atomofiron.insets.InsetsCombining
import lib.atomofiron.insets.composeInsets
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.insetsMargin
import lib.atomofiron.insets.insetsMix
import lib.atomofiron.insets.insetsPadding
import lib.atomofiron.insets.requestInsetOnLayoutChange

class DemoActivity : AppCompatActivity() {

    private val cutoutDrawable = CutoutDrawable()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        lib.atomofiron.insets.debugInsets = true

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)
            //root.foreground = cutoutDrawable

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top, panelsContainer::requestInsets)
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom, panelsContainer::requestInsets)
            switchConnection.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) topCtrl.show() else topCtrl.hide()
            }
            switchEat.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) bottomCtrl.show() else bottomCtrl.hide()
            }
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            var systemBarsBehavior = false
            switchFullscreen.setOnClickListener { switch ->
                switch as MaterialSwitch
                insetsController.run {
                    Type.systemBars().let { if (switch.isChecked) hide(it) else show(it) }
                }
                insetsController.systemBarsBehavior = when {
                    systemBarsBehavior -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    else -> WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
                if (switch.isChecked) {
                    systemBarsBehavior = !systemBarsBehavior
                }
            }
            toolbar.setNavigationOnClickListener { }
            fab.setOnClickListener {
                supportFragmentManager.run {
                    if (fragments.isNotEmpty()) return@run
                    beginTransaction()
                        .addToBackStack(null)
                        .setCustomAnimations(
                            R.anim.transition_scale_fade_enter,
                            R.anim.transition_scale_fade_exit,
                            R.anim.transition_scale_fade_pop_enter,
                            R.anim.transition_scale_fade_pop_exit,
                        )
                        .replace(R.id.fragments_container, PlayerFragment())
                        .commit()
                }
            }
            fab.setOnLongClickListener {
                snackbar = Snackbar.make(snackbarContainer, "Orientation-dependent snackbar", Snackbar.LENGTH_INDEFINITE).apply { show() }
                true
            }
            supportFragmentManager.addOnBackStackChangedListener {
                val show = supportFragmentManager.fragments.isEmpty()
                toolbar.isVisible = show
                if (show) fab.show() else {
                    fab.hide()
                    snackbar?.dismiss()
                }
            }
        }
    }

    private fun ActivityDemoBinding.configureInsets() {
        root.composeInsets(
            root.insetsPadding(ExtType.ime, bottom = true),
        ) { _, windowInsets -> // insets modifier
            syncCutout(windowInsets)
            ExtendedWindowInsets.Builder(windowInsets)
                .consume(windowInsets { ExtType.ime })
                .set(ExtType.fabTop, Insets.of(0, 0, 0, fab.visibleBottomHeight))
                .set(ExtType.fabHorizontal, Insets.of(fab.visibleLeftWidth, 0, fab.visibleRightWidth, 0))
                .build()
        }
        togglesContainer.composeInsets(
            bottomPanel.insetsPadding(horizontal = true, bottom = true)
                .dependency(vertical = true),
        ) { _, windowInsets ->
            switchFullscreen.isChecked = windowInsets.isEmpty(ExtType.systemBars)
            val insets = Insets.of(0, 0, 0, bottomPanel.visibleBottomHeight)
            ExtendedWindowInsets.Builder(windowInsets)
                .max(ExtType.general, insets)
                .set(ExtType.togglePanel, insets)
                .build()
        }
        val topDelegate = viewTop.insetsMix { margin(horizontal).padding(top) }
            .dependency(vertical = true)
        val bottomDelegate = viewBottom.insetsMix(ExtType.general) { horizontal(margin).bottom(padding) }
            .dependency(vertical = true)
        panelsContainer.composeInsets(topDelegate, bottomDelegate) { _, windowInsets ->
            val insets = Insets.of(0, viewTop.visibleTopHeight, 0, viewBottom.visibleBottomHeight)
            ExtendedWindowInsets.Builder(windowInsets)
                .max(ExtType.general, insets)
                .set(ExtType.verticalPanels, insets)
                .build()
        }
        toolbar.insetsMargin(ExtType.general, top = true, horizontal = true)
        val fabCombining = insetsCombining.copy(insetsCombining.combiningTypes + ExtType.togglePanel)
        fab.insetsMargin(ExtType { barsWithCutout + togglePanel + verticalPanels }, fabCombining, end = true, bottom = true)

        // this is needed because of fab is not a direct child of root insets provider
        root.requestInsetOnLayoutChange(fab, snackbarParentContainer)
        // nested container with applied insets
        val spcDelegate = snackbarParentContainer.insetsPadding(ExtType { barsWithCutout + togglePanel + verticalPanels })
        val spcCombining = InsetsCombining(ExtType.togglePanel, minBottom = resources.getDimensionPixelSize(R.dimen.common_padding))
        // snackbar dynamic relative position
        snackbarParentContainer.setInsetsModifier { _, windowInsets ->
            val landscape = snackbarParentContainer.run { width > height }
            spcDelegate.combining(spcCombining.takeIf { landscape })
            ExtendedWindowInsets.Builder(windowInsets)
                .run { if (landscape) consume(ExtType.fabTop) else consume(ExtType.fabHorizontal) }
                .build()
        }
        // child of nested container with decreased fab insets by consuming()
        snackbarContainer.insetsPadding(ExtType { fabTop + fabHorizontal }, bottom = true, end = true)
            .consuming(ExtType.general)
    }

    private fun syncCutout(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(Type.displayCutout())
        when {
            insets.left > 0 -> cutoutDrawable.left()
            insets.top > 0 -> cutoutDrawable.top()
            insets.right > 0 -> cutoutDrawable.right()
        }
    }
}