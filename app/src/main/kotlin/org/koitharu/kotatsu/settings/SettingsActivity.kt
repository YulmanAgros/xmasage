package org.koitharu.kotatsu.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.parser.external.ExternalMangaSource
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivitySettingsBinding
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.about.AboutSettingsFragment
import org.koitharu.kotatsu.settings.sources.SourceSettingsFragment
import org.koitharu.kotatsu.settings.sources.SourcesSettingsFragment
import org.koitharu.kotatsu.settings.sources.manage.SourcesManageFragment
import org.koitharu.kotatsu.settings.tracker.TrackerSettingsFragment
import org.koitharu.kotatsu.settings.userdata.UserDataSettingsFragment

@AndroidEntryPoint
class SettingsActivity :
	BaseActivity<ActivitySettingsBinding>(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
	AppBarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	private val isMasterDetails
		get() = viewBinding.containerMaster != null

	private var screenPadding = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySettingsBinding.inflate(layoutInflater))
		screenPadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		val currentFragment = fm.findFragmentById(R.id.container)
		if (currentFragment == null || (isMasterDetails && currentFragment is RootSettingsFragment)) {
			openDefaultFragment()
		}
		if (isMasterDetails && fm.findFragmentById(R.id.container_master) == null) {
			supportFragmentManager.commit {
				setReorderingAllowed(true)
				replace(R.id.container_master, RootSettingsFragment())
			}
		}
	}

	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference,
	): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
		fragment.arguments = pref.extras
		openFragment(fragment, isFromRoot = caller is RootSettingsFragment)
		return true
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.textViewHeader?.updateLayoutParams<MarginLayoutParams> {
			topMargin = screenPadding + insets.top
		}
	}

	fun setSectionTitle(title: CharSequence?) {
		viewBinding.textViewHeader?.apply {
			textAndVisible = title
		} ?: setTitle(title ?: getString(R.string.settings))
	}

	fun openFragment(fragment: Fragment, isFromRoot: Boolean) {
		val hasFragment = supportFragmentManager.findFragmentById(R.id.container) != null
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			if (!isMasterDetails || (hasFragment && !isFromRoot)) {
				addToBackStack(null)
			}
		}
	}

	private fun openDefaultFragment() {
		val fragment = when (intent?.action) {
			ACTION_READER -> ReaderSettingsFragment()
			ACTION_SUGGESTIONS -> SuggestionsSettingsFragment()
			ACTION_HISTORY -> UserDataSettingsFragment()
			ACTION_TRACKER -> TrackerSettingsFragment()
			ACTION_SOURCES -> SourcesSettingsFragment()
			ACTION_PROXY -> ProxySettingsFragment()
			ACTION_MANAGE_DOWNLOADS -> DownloadsSettingsFragment()
			ACTION_SOURCE -> SourceSettingsFragment.newInstance(
				MangaSource(intent.getStringExtra(EXTRA_SOURCE)),
			)

			ACTION_MANAGE_SOURCES -> SourcesManageFragment()
			Intent.ACTION_VIEW -> {
				when (intent.data?.host) {
					HOST_ABOUT -> AboutSettingsFragment()
					HOST_SYNC_SETTINGS -> SyncSettingsFragment()
					else -> null
				}
			}

			else -> null
		} ?: if (isMasterDetails) AppearanceSettingsFragment() else RootSettingsFragment()
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, fragment)
		}
	}

	companion object {

		private const val ACTION_READER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
		private const val ACTION_SUGGESTIONS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
		private const val ACTION_TRACKER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRACKER"
		private const val ACTION_HISTORY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_HISTORY"
		private const val ACTION_SOURCE = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
		private const val ACTION_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES"
		private const val ACTION_MANAGE_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES_LIST"
		private const val ACTION_MANAGE_DOWNLOADS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_DOWNLOADS"
		private const val ACTION_PROXY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_PROXY"
		private const val EXTRA_SOURCE = "source"
		private const val HOST_ABOUT = "about"
		private const val HOST_SYNC_SETTINGS = "sync-settings"

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_READER)

		fun newSuggestionsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SUGGESTIONS)

		fun newTrackerSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_TRACKER)

		fun newProxySettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_PROXY)

		fun newHistorySettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_HISTORY)

		fun newSourcesSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCES)

		fun newManageSourcesIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_SOURCES)

		fun newDownloadsSettingsIntent(context: Context) =
			Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_MANAGE_DOWNLOADS)

		fun newSourceSettingsIntent(context: Context, source: MangaSource): Intent = when (source) {
			is MangaSourceInfo -> newSourceSettingsIntent(context, source.mangaSource)
			is ExternalMangaSource -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
				.setData(Uri.fromParts("package", source.packageName, null))

			else -> Intent(context, SettingsActivity::class.java)
				.setAction(ACTION_SOURCE)
				.putExtra(EXTRA_SOURCE, source.name)
		}
	}
}
