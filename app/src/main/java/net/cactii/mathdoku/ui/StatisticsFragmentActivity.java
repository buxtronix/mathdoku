package net.cactii.mathdoku.ui;

import net.cactii.mathdoku.R;
import net.cactii.mathdoku.util.FeedbackEmail;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class StatisticsFragmentActivity extends AppFragmentActivity implements
		ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the statistics fragments. A
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative is used,
	 * which will keep every loaded fragment in memory.
	 */
	StatisticsFragmentPagerAdapter mStatisticsFragmentPagerAdapter;

	/**
	 * The {@link ViewPager} that will display the statistics fragments , one at
	 * a time.
	 */
	ViewPager mViewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.statistics_activity_fragment);

		// Create the adapter that will return the statistics fragments.
		mStatisticsFragmentPagerAdapter = new StatisticsFragmentPagerAdapter(
				getSupportFragmentManager());

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(getResources().getString(
				R.string.statistics_actionbar_title));

		// Set up the ViewPager, attaching the adapter and setting up a listener
		// for when the user swipes between the statistics fragments.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mStatisticsFragmentPagerAdapter);
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between different statistics fragments,
						// select the corresponding tab.
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the statistics fragments, add a tab to the action bar.
		for (int i = 0; i < mStatisticsFragmentPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the listener for when this tab is
			// selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mStatisticsFragmentPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		// Show the same page as last time (or the last tab if statistics were
		// not displayed before.
		int tab = mMathDokuPreferences.getStatisticsTabLastDisplayed();
		mViewPager.setCurrentItem(tab >= 0 ? tab
				: mStatisticsFragmentPagerAdapter.getCount() - 1);

		/*
		 * Styling of the pager tab strip is not possible from within code. See
		 * values-v14/styles.xml for styling of the action bar tab.
		 */
	}

	@Override
	protected void onPause() {
		// Store tab which is currently displayed.
		mMathDokuPreferences.setStatisticsTabLastDisplayed(mViewPager
				.getCurrentItem());
		super.onPause();
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.statistics_menu, menu);
		return true;
	}

	private enum MenuId {
		HOME(android.R.id.home),
		STATISTICS_SETTINGS(R.id.statistics_settings),
		ACTION_SEND_FEEDBACK(R.id.action_send_feedback),
		ACTION_HELP(R.id.action_help),
		OTHER(0);

		private final int value;

		MenuId(int value) {
			this.value = value;
		}

		public static MenuId fromId(int id){
			for(MenuId e: MenuId.values()){
				if(e.value == id){
					return e;
				}
			}
			return OTHER;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (MenuId.fromId(item.getItemId())) {
			case HOME:
				// This is called when the Home (Up) button is pressed in the action
				// bar. Create a simple intent that starts the hierarchical parent
				// activity and use NavUtils in the Support Package to ensure proper
				// handling of Up.
				Intent upIntent = new Intent(this, PuzzleFragmentActivity.class);
				if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
					// This activity is not part of the application's task, so
					// create a new task with a synthesized back stack.
					// If there are ancestor activities, they should be added here.
					TaskStackBuilder.create(this).addNextIntent(upIntent)
							.startActivities();
					finish();
				} else {
					// This activity is part of the application's task, so simply
					// navigate up to the hierarchical parent activity.
					NavUtils.navigateUpTo(this, upIntent);
				}
				return true;
			case STATISTICS_SETTINGS:
				startActivity(new Intent(this, StatisticsPreferenceActivity.class));
				break;
			case ACTION_SEND_FEEDBACK:
				new FeedbackEmail(this).show();
				return true;
			case ACTION_HELP:
				openHelpDialog();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Displays the Help Dialog.
	 */
	private void openHelpDialog() {
		// Get view and put relevant information into the view.
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.statistics_help_dialog, null);

		new AlertDialog.Builder(this)
				.setTitle(
						getResources().getString(R.string.action_statistics)
								+ " "
								+ getResources()
										.getString(R.string.action_help))
				.setIcon(R.drawable.icon)
				.setView(view)
				.setPositiveButton(R.string.dialog_general_button_close,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing
							}
						}).show();
	}
}