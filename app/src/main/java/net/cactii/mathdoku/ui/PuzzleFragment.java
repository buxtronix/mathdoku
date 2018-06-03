package net.cactii.mathdoku.ui;

import java.util.ArrayList;

import net.cactii.mathdoku.Cheat;
import net.cactii.mathdoku.Cheat.CheatType;
import net.cactii.mathdoku.GameTimer;
import net.cactii.mathdoku.Preferences;
import net.cactii.mathdoku.R;
import net.cactii.mathdoku.grid.CellChange;
import net.cactii.mathdoku.grid.Grid;
import net.cactii.mathdoku.grid.GridCage;
import net.cactii.mathdoku.grid.GridCell;
import net.cactii.mathdoku.grid.ui.GridInputMode;
import net.cactii.mathdoku.grid.ui.GridPlayerView;
import net.cactii.mathdoku.hint.TickerTape;
import net.cactii.mathdoku.painter.Painter;
import net.cactii.mathdoku.statistics.GridStatistics.StatisticsCounterType;
import net.cactii.mathdoku.tip.TipCheat;
import net.cactii.mathdoku.tip.TipDialog;
import net.cactii.mathdoku.tip.TipIncorrectValue;
import net.cactii.mathdoku.util.Util;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Paul
 * 
 */
public class PuzzleFragment extends android.support.v4.app.Fragment implements
		OnSharedPreferenceChangeListener, OnCreateContextMenuListener,
		GridPlayerView.OnInputModeChangedListener {
	public final static String TAG = "MathDoku.PuzzleFragment";

	public static final String BUNDLE_KEY_SOLVING_ATTEMPT_ID = "PuzzleFragment.solvingAttemptId";

	// The grid and the view which will display the grid.
	public Grid mGrid;
	public GridPlayerView mGridPlayerView;

	// A global painter object to paint the grid in different themes.
	public Painter mPainter;

	private GameTimer mTimerTask;

	private RelativeLayout mPuzzleGridLayout;
	private TextView mTimerText;
	private ImageView mInputModeImageView;
	private TextView mInputModeText;

	private TickerTape mTickerTape;

	private LinearLayout mButtonsLayout;
	private Button mDigit1;
	private Button mDigit2;
	private Button mDigit3;
	private Button mDigit4;
	private Button mDigit5;
	private Button mDigit6;
	private Button mDigit7;
	private Button mDigit8;
	private Button mDigit9;
	private ImageButton mDigitC; // Clear.
	private ImageButton mDigitM; // Maybe.
	public ImageButton mDigitU; // Undo.
	
	public LinearLayout mClearUndo;
	private Button mClearButton;
	private Button mUndoButton;
	private View[] mSoundEffectViews;

	public Preferences mMathDokuPreferences;

	private Context mContext;

	private View mRootView;

	private OnGridFinishedListener mOnGridFinishedListener;

	private BroadcastReceiver mDreamingBroadcastReceiver;

	// Container Activity must implement these interfaces
	public interface OnGridFinishedListener {
		public void onGridFinishedListener(int solvingAttemptId);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mOnGridFinishedListener = (OnGridFinishedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnGridFinishedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater
				.inflate(R.layout.puzzle_fragment, container, false);

		mContext = getActivity();

		mPainter = Painter.getInstance();
		mMathDokuPreferences = Preferences.getInstance();
		mMathDokuPreferences.mSharedPreferences
				.registerOnSharedPreferenceChangeListener(this);

		mPuzzleGridLayout = (RelativeLayout) mRootView
				.findViewById(R.id.puzzleGrid);
		mGridPlayerView = (GridPlayerView) mRootView
				.findViewById(R.id.grid_player_view);
		mTimerText = (TextView) mRootView.findViewById(R.id.timerText);

		mButtonsLayout = (LinearLayout)mRootView.findViewById(R.id.digitButtons);
		mDigit1 = (Button)mRootView.findViewById(R.id.digit1);
		mDigit2 = (Button)mRootView.findViewById(R.id.digit2);
		mDigit3 = (Button)mRootView.findViewById(R.id.digit3);
		mDigit4 = (Button)mRootView.findViewById(R.id.digit4);
		mDigit5 = (Button)mRootView.findViewById(R.id.digit5);
		mDigit6 = (Button)mRootView.findViewById(R.id.digit6);
		mDigit7 = (Button)mRootView.findViewById(R.id.digit7);
		mDigit8 = (Button)mRootView.findViewById(R.id.digit8);
		mDigit9 = (Button)mRootView.findViewById(R.id.digit9);
		mDigitC = (ImageButton)mRootView.findViewById(R.id.digitC);
		mDigitM = (ImageButton)mRootView.findViewById(R.id.digitM);
		mDigitU = (ImageButton)mRootView.findViewById(R.id.digitU);
		for (View button : allButtons(false)) {
			button.setBackgroundColor(mPainter.getButtonBackgroundColor());
		}
		
		mClearButton = (Button) mRootView.findViewById(R.id.clearButton);
		mClearButton.setBackgroundColor(mPainter.getButtonBackgroundColor());

		mUndoButton = (Button) mRootView.findViewById(R.id.undoButton);
		mUndoButton.setBackgroundColor(mPainter.getButtonBackgroundColor());
		
		mClearUndo = (LinearLayout) mRootView.findViewById(R.id.clearUndo);

		mTickerTape = (TickerTape) mRootView.findViewById(R.id.tickerTape);
		mGridPlayerView.setTickerTape(mTickerTape);

		mSoundEffectViews = new View[] { mGridPlayerView, mClearButton,
				mUndoButton };

		// Hide all controls until sure a grid view can be displayed.
		setNoGridLoaded();

		mGridPlayerView
				.setOnGridTouchListener(mGridPlayerView.new OnGridTouchListener() {
					@Override
					public void gridTouched(GridCell cell) {
						setClearAndUndoButtonVisibility(cell);
					}
				});
		mClearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGridPlayerView != null) {
					mGridPlayerView.digitSelected(0);

					setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
					mGridPlayerView.invalidate();
				}
			}
		});
		mUndoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGrid.undoLastMove()) {
					// Successful undo
					setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
					mGridPlayerView.invalidate();

					// Undo can toggle the visibility of the check progress
					// button in the action bar
					((FragmentActivity) mContext).invalidateOptionsMenu();
				}
			}
		});

		mDigit1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(1);
			}
		});
		mDigit2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(2);
			}
		});
		mDigit3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(3);
			}
		});
		mDigit4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(4);
			}
		});
		mDigit5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(5);
			}
		});
		mDigit6.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(6);
			}
		});
		mDigit7.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(7);
			}
		});
		mDigit8.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(8);
			}
		});
		mDigit9.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setDigitSelected(9);
			}
		});
		mDigitC.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGridPlayerView != null) {
					mGridPlayerView.digitSelected(0);
					mGridPlayerView.invalidate();
					setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
				}
			}
		});
		if (mDigitM != null) {
			mDigitM.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mGridPlayerView != null && mGrid != null) {
						// Toggle input mode
						mGridPlayerView.toggleInputMode();
					}
				}
			});
		}
		if (mDigitU != null) {
			mDigitU.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mGrid.undoLastMove()) {
						// Successful undo
						setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
						mGridPlayerView.invalidate();
	
						// Undo can toggle the visibility of the check progress
						// button in the action bar
						((FragmentActivity) mContext).invalidateOptionsMenu();
					}
				}
			});
		}

		mGridPlayerView.setFocusable(true);
		mGridPlayerView.setFocusableInTouchMode(true);

		// Initialize the input mode
		mInputModeImageView = (ImageView) mRootView
				.findViewById(R.id.input_mode_image);
		mInputModeText = (TextView) mRootView
				.findViewById(R.id.input_mode_text);
		setInputModeTextVisibility();
		mInputModeText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGridPlayerView != null && mGrid != null) {
					// Toggle input mode
					mGridPlayerView.toggleInputMode();
				}
			}
		});
		
		mInputModeImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mGridPlayerView != null && mGrid != null) {
					// Toggle input mode
					mGridPlayerView.toggleInputMode();
				}
			}
		});
		mGridPlayerView.setOnInputModeChangedListener(this);
		// Force display of normal mode text.
		this.onInputModeChanged(GridInputMode.NORMAL);

		registerForContextMenu(mGridPlayerView);

		// In case a solving attempt id has been passed, this attempt has to be
		// loaded.
		Bundle args = getArguments();
		if (args != null) {
			int solvingAttemptId = args.getInt(BUNDLE_KEY_SOLVING_ATTEMPT_ID);

			mGrid = new Grid();
			if (mGrid.load(solvingAttemptId)) {
				setNewGrid(mGrid);
				if (mGrid.mMaybeMode) {
					this.onInputModeChanged(GridInputMode.MAYBE);
				}
			}
		}

		return mRootView;
	}

	@Override
	public void onPause() {
		// Store preference counters as they are kept in internal memory for
		// performance reasons.
		if (mMathDokuPreferences != null) {
			mMathDokuPreferences.commitCounters();
		}

		// Unregister the receiver of the day dreaming intent.
		if (mContext != null && mDreamingBroadcastReceiver != null) {
			mContext.unregisterReceiver(mDreamingBroadcastReceiver);
		}

		pause();

		super.onPause();
	}

	public void setTheme() {
		mPainter.setTheme(mMathDokuPreferences.getTheme());

		// Invalidate the grid view and the input mode button in order to used
		// the new theme setting
		if (mGridPlayerView != null) {
			mGridPlayerView.invalidate();
			if (mInputModeImageView != null) {
				setInputModeImage(mGridPlayerView.getGridInputMode());
				mInputModeImageView.invalidate();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mMathDokuPreferences != null
				&& mMathDokuPreferences.mSharedPreferences != null) {
			mMathDokuPreferences.mSharedPreferences
					.unregisterOnSharedPreferenceChangeListener(this);
		}
		super.onDestroy();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onResume() {
		// Register a broadcast receiver on the intents related to day dreaming.
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			// Create broad cast receiver
			if (mDreamingBroadcastReceiver == null) {
				mDreamingBroadcastReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if (intent.getAction().equals(
								Intent.ACTION_DREAMING_STARTED)) {
							// Pause the fragment on start of dreaming
							pause();
						} else if (intent.getAction().equals(
								Intent.ACTION_DREAMING_STOPPED)) {
							// Resume the fragment as soon as the dreaming has
							// stopped
							resume();
						}
					}
				};
			}

			// Create intent filter for day dreaming
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_DREAMING_STARTED);
			intentFilter.addAction(Intent.ACTION_DREAMING_STOPPED);

			// Register receiver
			mContext.registerReceiver(mDreamingBroadcastReceiver, intentFilter,
					null, null);
		}

		// Resume the fragment
		resume();

		super.onResume();
	}

	public void setSoundEffectsEnabled(boolean enabled) {
		for (View v : this.mSoundEffectViews)
			v.setSoundEffectsEnabled(enabled);
	}

	/**
	 * Checks whether the clear grid menu item is available.
	 * 
	 * @return True in case the clear grid menu item is available. False
	 *         otherwise.
	 */
	protected boolean showClearGrid() {
		return (mGrid != null && mGrid.isActive() && mGrid.isEmpty(true) == false);
	}

	/**
	 * Handles clearing of the entire grid. The grid will only be cleared after
	 * the user has confirmed clearing.
	 */
	protected void clearGrid() {
		new AlertDialog.Builder(this.getActivity())
				.setTitle(R.string.dialog_clear_grid_confirmation_title)
				.setMessage(R.string.dialog_clear_grid_confirmation_message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNegativeButton(
						R.string.dialog_clear_grid_confirmation_negative_button,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								//
							}
						})
				.setPositiveButton(
						R.string.dialog_clear_grid_confirmation_positive_button,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								PuzzleFragment.this.mGrid.clearCells(false);
								PuzzleFragment.this.mGridPlayerView
										.invalidate();
								// Invalidate the option menu to hide the check
								// progress action if necessary
								((FragmentActivity) mContext)
										.invalidateOptionsMenu();
								setClearAndUndoButtonVisibility(null);
							}
						}).show();
	}

	/**
	 * Sets the handler on the grid which will be called as soon as the grid has
	 * been solved.
	 */
	public void setOnSolvedHandler() {
		if (mGrid == null) {
			return;
		}

		mGrid.setSolvedHandler(mGrid.new OnSolvedListener() {
			@Override
			public void puzzleSolved() {
				// Stop the time and unselect the current cell and cage. Finally
				// save the grid.
				stopTimer();
				mGrid.deselectSelectedCell();
				mGrid.save();

				// Notify the containing fragment activity about the finishing
				// of the grid. In case the puzzle has been solved manually, a
				// animation is played first.
				if (!mGrid.isActive() || mGrid.isSolutionRevealed()
						|| mGrid.countMoves() == 0) {
					mOnGridFinishedListener.onGridFinishedListener(mGrid
							.getSolvingAttemptId());
				} else {
					// Hide controls while showing the animation.
					setInactiveGridLoaded();

					// Set the text view which will be animated
					final TextView textView = (TextView) mRootView
							.findViewById(R.id.solvedText);
					textView.setText(R.string.main_ui_solved_messsage);
					textView.setTextColor(Painter.getInstance()
							.getSolvedTextPainter().getTextColor());
					textView.setBackgroundColor(Painter.getInstance()
							.getSolvedTextPainter().getBackgroundColor());
					textView.setTypeface(Painter.getInstance().getTypeface());
					textView.setVisibility(View.VISIBLE);

					// Build the animation
					Animation animation = AnimationUtils.loadAnimation(
							mContext, R.anim.solved);
					animation.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationEnd(Animation animation) {
							textView.setVisibility(View.GONE);
							mOnGridFinishedListener
									.onGridFinishedListener(mGrid
											.getSolvingAttemptId());
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationStart(Animation animation) {
						}
					});

					// Start animation of the text view.
					textView.startAnimation(animation);
				}

			}
		});
	}

	/**
	 * Load the new grid and set control visibility.
	 * 
	 * @param grid
	 *            The grid to display.
	 */
	public void setNewGrid(Grid grid) {
		if (grid != null) {
			mGrid = grid;
			mGridPlayerView.loadNewGrid(grid);

			// Show the grid of the loaded puzzle.
			if (mGrid.isActive()) {
				// Set visibility of grid layout
				if (mPuzzleGridLayout != null) {
					mPuzzleGridLayout.setVisibility(View.VISIBLE);
					mPuzzleGridLayout.invalidate();
				}

				// Set timer
				if (mMathDokuPreferences.isTimerVisible() && mTimerText != null) {
					mTimerText.setVisibility(View.VISIBLE);
					mTimerText.invalidate();
				}
				startTimer();

				setClearAndUndoButtonVisibility((mGrid == null ? null : mGrid
						.getSelectedCell()));
				
				setDigitButtons();

				// Handler for solved game
				setOnSolvedHandler();
			} else {
				setInactiveGridLoaded();
				stopTimer();
			}

			// Check actionbar and menu options
			((Activity) mContext).invalidateOptionsMenu();

			mRootView.invalidate();
		} else {
			// No grid available.
			setNoGridLoaded();
		}
	}
	
	private ArrayList<View> allButtons(boolean onlyDigits) {
		ArrayList<View> usedbuttons = new ArrayList<View>();
		View[] buttons = {mDigit1, mDigit2, mDigit3, mDigit4, mDigit5, mDigit6,
				          mDigit7, mDigit8, mDigit9, mDigitC, mDigitM, mDigitU};
		int length;
		if (onlyDigits) {
			length = 9;
		} else {
			length = buttons.length;
		}
		for (int i=0 ; i < length ; i++) {
			if (buttons[i] != null) {
				usedbuttons.add(buttons[i]);
			}
		}
		return usedbuttons;
	}
	
	/**
	 * Set the correct visibility of the digit select buttons.
	 */
	private void setDigitButtons() {
		boolean sound = mMathDokuPreferences.isPlaySoundEffectEnabled();
		for (View button : allButtons(false)) {
			button.setSoundEffectsEnabled(sound);
		}

		if (mMathDokuPreferences.isDigitButtonsVisible()) {
			mDigit5.setVisibility(View.INVISIBLE);
			mDigit6.setVisibility(View.INVISIBLE);
			mDigit7.setVisibility(View.INVISIBLE);
			mDigit8.setVisibility(View.INVISIBLE);
			mDigit9.setVisibility(View.INVISIBLE);

			switch (mGrid.getGridSize()) {
			case 9:
				mDigit9.setVisibility(View.VISIBLE);
			case 8:
				mDigit8.setVisibility(View.VISIBLE);
			case 7:
				mDigit7.setVisibility(View.VISIBLE);
			case 6:
				mDigit6.setVisibility(View.VISIBLE);
			case 5:
				mDigit5.setVisibility(View.VISIBLE);				
			}
			setDigitButtonsMode();
			mButtonsLayout.setVisibility(View.VISIBLE);
			mTickerTape.setDisabled(true);
			for (View button : allButtons(false)) {
				button.invalidate();
			}
			mButtonsLayout.invalidate();
			//mUndoButton.setVisibility(View.GONE);
			//mClearButton.setVisibility(View.GONE);
			mClearUndo.setVisibility(View.GONE);
		} else {
			mButtonsLayout.setVisibility(View.GONE);
			mTickerTape.setDisabled(false);
			//mUndoButton.setVisibility(View.VISIBLE);
			//mClearButton.setVisibility(View.VISIBLE);
			mClearUndo.setVisibility(View.VISIBLE);
		}
		setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
	}
	
	/**
	 * Set the digit buttons colours base on input mode.
	 */
	private void setDigitButtonsMode() {
		if (mGridPlayerView.getGridInputMode() == GridInputMode.NORMAL) {
			for (View button : allButtons(true)) {
				Button tb = (Button)button;
				tb.setTextColor(mPainter.getDigitFgColor());;
			}
			if (mDigitM != null) {
				mDigitM.setColorFilter(0x00000000);
			}
		} else {
			for (View button : allButtons(true)) {
				Button tb = (Button)button;
				tb.setTextColor(mPainter.getDigitFgMaybeColor());;
			}
			if (mDigitM != null) {
				mDigitM.setColorFilter(0xaaffc000);
			}
		}
	}

	/**
	 * Start a new timer (only in case the grid is active).
	 */
	protected void startTimer() {
		// Stop old timer
		stopTimer();

		if (mGrid != null && mGrid.isActive()) {
			mTimerTask = new GameTimer(this);
			mTimerTask.mElapsedTime = mGrid.getElapsedTime();
			mTimerTask.mCheatPenaltyTime = mGrid.getCheatPenaltyTime();
			if (mMathDokuPreferences.isTimerVisible()) {
				mTimerText.setVisibility(View.VISIBLE);
			} else {
				mTimerText.setVisibility(View.INVISIBLE);
			}
			mTimerTask.execute();
		}
	}

	/**
	 * Stop the current timer.
	 */
	public void stopTimer() {
		// Stop timer if running
		if (mTimerTask != null && !mTimerTask.isCancelled()) {
			if (mGrid != null) {
				this.mGrid.setElapsedTime(mTimerTask.mElapsedTime,
						mTimerTask.mCheatPenaltyTime);
			}
			mTimerTask.cancel(true);
		}
	}

	/**
	 * Sets the timer text with the actual elapsed time.
	 * 
	 * @param elapsedTime
	 *            The elapsed time (in mili seconds) while playing the game.
	 */
	@SuppressLint("DefaultLocale")
	public void setElapsedTime(long elapsedTime) {
		if (mTimerText != null) {
			mTimerText.setText(Util.durationTimeToString(elapsedTime));
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(Preferences.PUZZLE_SETTING_THEME)) {
			setTheme();
		}
		if (key.equals(Preferences.PUZZLE_SETTING_DIGIT_INPUT_TYPE)) {
			setDigitButtons();
		}
	}

	/**
	 * Checks whether the reveal cell menu item is available.
	 * 
	 * @return
	 */
	protected boolean showRevealCell() {
		return (mGrid != null && mGrid.isActive() && mGrid.getSelectedCell() != null);
	}

	/**
	 * Handles revealing of user value in the given cell.
	 * 
	 * @param selectedCell
	 *            The cell for which the user value has to be revealed.
	 */
	protected void revealCell() {
		if (mGrid == null) {
			return;
		}

		GridCell selectedCell = mGrid.getSelectedCell();
		if (selectedCell == null) {
			return;
		}

		// Save old cell info
		CellChange orginalUserMove = selectedCell.saveUndoInformation(null);

		// Reveal the user value
		selectedCell.setRevealed();
		selectedCell.setUserValue(selectedCell.getCorrectValue());
		if (mMathDokuPreferences.isPuzzleSettingClearMaybesEnabled()) {
			// Update possible values for other cells in this row and
			// column.
			mGrid.clearRedundantPossiblesInSameRowOrColumn(orginalUserMove);
		}
		setClearAndUndoButtonVisibility(selectedCell);

		mGrid.increaseCounter(StatisticsCounterType.ACTION_REVEAL_CELL);
		Cheat cheat = getNewCheat(CheatType.CELL_REVEALED);

		// Display tip
		if (TipCheat.toBeDisplayed(mMathDokuPreferences, cheat)) {
			new TipCheat(mContext, cheat).show();
		}

		this.mGridPlayerView.invalidate();
	}

	/**
	 * Checks whether the reveal operator menu item is available.
	 * 
	 * @return True in case the reveal operator menu item is available. False
	 *         otherwise.
	 */
	protected boolean showRevealOperator() {
		if (mGrid == null || mGrid.isActive() == false) {
			return false;
		}

		// Determine current selected cage.
		GridCage selectedGridCage = mGrid.getCageForSelectedCell();
		return (selectedGridCage != null && selectedGridCage.isOperatorHidden());
	}

	/**
	 * Handles revealing of the operator of the given cage.
	 */
	protected void revealOperator() {
		if (mGrid == null || mGrid.isActive() == false) {
			return;
		}

		// Determine current selected cage.
		GridCage selectedGridCage = mGrid.getCageForSelectedCell();

		if (selectedGridCage == null) {
			return;
		}

		selectedGridCage.revealOperator();

		mGrid.increaseCounter(StatisticsCounterType.ACTION_REVEAL_OPERATOR);
		Cheat cheat = getNewCheat(CheatType.OPERATOR_REVEALED);

		// Display tip
		if (TipCheat.toBeDisplayed(mMathDokuPreferences, cheat)) {
			new TipCheat(mContext, cheat).show();
		}

		mGridPlayerView.invalidate();
	}

	/**
	 * Create a cheat of the given type.
	 * 
	 * @param cheatType
	 *            The type of cheat to be processed.
	 */
	private Cheat getNewCheat(CheatType cheatType) {
		// Create new cheat
		Cheat cheat = new Cheat(mContext, cheatType);

		// Add penalty time
		if (mTimerTask != null) {
			mTimerTask.addCheatPenaltyTime(cheat);
		}

		return cheat;
	}

	/**
	 * Checks whether the check progress menu item is available.
	 * 
	 * @return
	 */
	protected boolean showCheckProgress() {
		return (mGrid != null && mGrid.isActive() && !mGrid.isEmpty(false));
	}

	/**
	 * Checks the progress of solving the current grid
	 */
	protected void checkProgress() {
		if (mGrid == null || mGridPlayerView == null) {
			return;
		}

		boolean allUserValuesValid = mGrid.isSolutionValidSoFar();
		int countNewInvalidChoices = (allUserValuesValid ? 0 : mGridPlayerView
				.markInvalidChoices());

		// Create new cheat
		Cheat cheat = new Cheat(this.getActivity(),
				CheatType.CHECK_PROGRESS_USED, countNewInvalidChoices);

		// Register cheat in statistics
		mGrid.getGridStatistics().increaseCounter(
				StatisticsCounterType.ACTION_CHECK_PROGRESS);
		mGrid.getGridStatistics().increaseCounter(
				StatisticsCounterType.CHECK_PROGRESS_INVALIDS_CELLS_FOUND,
				countNewInvalidChoices);

		// Add penalty time
		if (mTimerTask != null) {
			mTimerTask.addCheatPenaltyTime(cheat);
		}

		// Display tip or toast
		if (TipCheat.toBeDisplayed(mMathDokuPreferences, cheat)) {
			new TipCheat(mContext, cheat).show();
		} else if (allUserValuesValid) {
			Toast.makeText(mContext, R.string.ProgressOK, Toast.LENGTH_SHORT)
					.show();
		} else {
			Toast.makeText(mContext, R.string.ProgressBad, Toast.LENGTH_SHORT)
					.show();
		}

		// Never show the tip about incorrect values and reference to function
		// Check Progress again.
		TipIncorrectValue.doNotDisplayAgain(mMathDokuPreferences);
	}

	/**
	 * Checks whether the clear and undo buttons should be visible in case the
	 * given cell is selected.
	 * 
	 * @param cell
	 *            The cell to be used to check whether the clear and undo button
	 *            should be visible. Use null in case no cell is selected.
	 */
	@SuppressLint("NewApi") private void setClearAndUndoButtonVisibility(GridCell cell) {
		boolean digitButtons = mMathDokuPreferences.isDigitButtonsVisible();
		boolean hideClear = cell == null || cell.isEmpty();
		boolean hideUndo = mGrid == null || mGrid.countMoves() == 0 || mGrid.isActive() == false;
		if (digitButtons) {
			if (mDigitC != null) {
				mDigitC.setEnabled(!hideClear);
				mDigitC.setBackgroundColor(hideClear ? mPainter.getButtonInactiveBackgroundColor() :
														mPainter.getButtonBackgroundColor());
			}
			if (mDigitU != null) {
				mDigitU.setEnabled(!hideUndo);
				mDigitU.setBackgroundColor(hideUndo ? mPainter.getButtonInactiveBackgroundColor() :
					mPainter.getButtonBackgroundColor());
			}
		} else {
			if (mClearButton != null) {
				mClearButton.setBackgroundColor(hideClear ? mPainter.getButtonInactiveBackgroundColor() :
					mPainter.getButtonBackgroundColor());
				mClearButton.setEnabled(!hideClear);
			}
			if (mUndoButton != null) {
				mUndoButton.setBackgroundColor(hideUndo ? mPainter.getButtonInactiveBackgroundColor() :
					mPainter.getButtonBackgroundColor());
				mUndoButton.setEnabled(!hideUndo);
			}
		}
		GetAvailableDimensions();
	}

	private int PxToDp(int px) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		return (int) (px / (metrics.densityDpi/160f));
	}
	
	@SuppressLint("NewApi") private void GetAvailableDimensions() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int screenHeightDp = PxToDp(metrics.heightPixels);
		int screenWidthDp = PxToDp(metrics.widthPixels);
		
		// Reduce 56dp due to action bar.
		screenHeightDp -= 56;
		// reduce due to mode bar.
		screenHeightDp -= 25;
		// 48 dp less if using soft navbar.
		ViewConfiguration vc = ViewConfiguration.get(mContext);
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			if (vc.hasPermanentMenuKey()) {
				screenHeightDp -= 48;
			}
		}
		// 25dp less if not full screen.
		if (!mMathDokuPreferences.isFullScreenEnabled()) {
			screenHeightDp -= 25;
		}
		int avail = screenHeightDp - screenWidthDp;
		if (avail > 140) {
			return;
		} else if (avail > 120) {
			for (View btn : allButtons(false)) {
				btn.setPadding(8, 8, 8, 8);
				btn.invalidate();
			}
		}
	}
	
	public boolean isActive() {
		return (mGrid != null && mGrid.isActive());
	}

	public void prepareLoadNewGame() {
		// Cancel old timer if running.
		stopTimer();

		// Save the game.
		if (mGrid != null) {
			mGrid.save();
		}
	}

	/**
	 * Checks whether the reveal solution menu item is available.
	 * 
	 * @return True in case the reveal solution menu item is available. False
	 *         otherwise.
	 */
	protected boolean showRevealSolution() {
		return (mGrid != null && mGrid.isActive());
	}

	/**
	 * Handles revealing of the solution of the grid.
	 */
	protected void revealSolution() {
		if (mGrid == null) {
			return;
		}

		new AlertDialog.Builder(this.getActivity())
				.setTitle(R.string.dialog_reveal_solution_confirmation_title)
				.setMessage(
						R.string.dialog_reveal_solution_confirmation_message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNegativeButton(
						R.string.dialog_reveal_solution_confirmation_negative_button,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// do nothing
							}
						})
				.setPositiveButton(
						R.string.dialog_reveal_solution_confirmation_positive_button,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Disable the solved listener before revealing
								// the solution.
								mGrid.setSolvedHandler(null);

								// Reveal the solution
								mGrid.revealSolution();

								// Create the cheat. This also updates the cheat
								// penalty in the timer.
								Cheat cheat = getNewCheat(CheatType.SOLUTION_REVEALED);

								// Stop the timer and unselect the current cell
								// and cage. Finally save the grid.
								stopTimer();
								mGrid.deselectSelectedCell();
								mGrid.save();

								// Check if tip has to be displayed before
								// informing the listener about finishing the
								// grid.
								if (cheat != null
										&& TipCheat.toBeDisplayed(
												mMathDokuPreferences, cheat)) {
									new TipCheat(mContext, cheat)
											.setOnClickCloseListener(
													new TipDialog.OnClickCloseListener() {
														@Override
														public void onTipDialogClose() {
															// Notify the
															// containing
															// fragment
															// activity
															// about the
															// finishing
															// of the grid.
															// In case the
															// puzzle has
															// been solved
															// manually, a
															// animation is
															// played first.
															if (mGrid != null
																	&& mOnGridFinishedListener != null) {
																mOnGridFinishedListener
																		.onGridFinishedListener(mGrid
																				.getSolvingAttemptId());
															}
														};
													}).show();
								} else {
									if (mGrid != null
											&& mOnGridFinishedListener != null) {
										mOnGridFinishedListener
												.onGridFinishedListener(mGrid
														.getSolvingAttemptId());
									}
								}
							}
						}).show();
	}

	/**
	 * Set visibility of controls for the case no grid could be loaded.
	 */
	private void setNoGridLoaded() {
		if (mPuzzleGridLayout != null) {
			mPuzzleGridLayout.setVisibility(View.GONE);
		}
		if (mTimerText != null) {
			mTimerText.setVisibility(View.GONE);
		}
		/*
		if (mClearButton != null) {
			mClearButton.setVisibility(View.GONE);
		}
		if (mUndoButton != null) {
			mUndoButton.setVisibility(View.GONE);
		}
		*/
		mClearUndo.setVisibility(View.GONE);
		if (mButtonsLayout != null) {
			mButtonsLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Set visibility of controls for an inactive grid.
	 */
	private void setInactiveGridLoaded() {
		if (mPuzzleGridLayout != null) {
			mPuzzleGridLayout.setVisibility(View.VISIBLE);
		}
		if (mGrid == null || (mGrid != null && mGrid.isSolutionRevealed())) {
			// Hide time in case the puzzle was solved by
			// requesting to show the solution.
			if (mTimerText != null) {
				mTimerText.setVisibility(View.INVISIBLE);
				mTimerText.invalidate();
			}
		} else {
			// Show time
			if (mTimerText != null) {
				mTimerText.setVisibility(View.VISIBLE);
				mTimerText.invalidate();
				setElapsedTime(mGrid.getElapsedTime());
			}
		}
		if (mInputModeImageView != null) {
			mInputModeImageView.setVisibility(View.INVISIBLE);
			mInputModeImageView.invalidate();
		}
		mClearUndo.setVisibility(View.GONE);
		if (mButtonsLayout != null) {
			mButtonsLayout.setVisibility(View.GONE);
			mButtonsLayout.invalidate();
		}
	}

	/**
	 * Get the solving attempt id which is being showed in this archive
	 * fragment.
	 * 
	 * @return The solving attempt id which is being showed in this archive
	 *         fragment.
	 */
	public int getSolvingAttemptId() {
		return mGrid.getSolvingAttemptId();
	}

	/**
	 * Checks whether the normal input mode menu item is visible.
	 * 
	 * @return
	 */
	protected boolean showInputModeNormal() {
		return (mGrid != null && mGrid.isActive() && mGridPlayerView != null && mGridPlayerView
				.getGridInputMode() == GridInputMode.MAYBE);
	}

	/**
	 * Checks whether the maybe input mode menu item is visible.
	 * 
	 * @return
	 */
	protected boolean showInputModeMaybe() {
		return (mGrid != null && mGrid.isActive() && mGridPlayerView != null && mGridPlayerView
				.getGridInputMode() == GridInputMode.NORMAL);
	}

	@Override
	public void onInputModeChanged(GridInputMode inputMode) {
		setInputModeImage(inputMode);
		setDigitButtonsMode();
		// Display message
		mInputModeText.setVisibility(View.VISIBLE);
		
		if (mMathDokuPreferences.increaseInputModeChangedCounter() < 4 &&
				!mMathDokuPreferences.isDigitButtonsVisible()) {
			mInputModeText
			.setText(inputMode == GridInputMode.NORMAL ? R.string.input_mode_changed_to_normal
					: R.string.input_mode_changed_to_maybe);
		} else {
			mInputModeText
			.setText(inputMode == GridInputMode.NORMAL ? R.string.input_mode_normal
					: R.string.input_mode_maybe);
		}
		mInputModeText.invalidate();
		if (mGrid != null) {
			mGrid.mMaybeMode = (inputMode == GridInputMode.MAYBE);
		}
	}

	/**
	 * Set the visibility of the input mode text to invisible or gone.
	 */
	private void setInputModeTextVisibility() {
		if (mInputModeText != null) {
			mInputModeText.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Set the input mode image for the given input mode.
	 * 
	 * @param inputMode
	 *            The input mode for which the input mode button has to be set.
	 */
	private void setInputModeImage(GridInputMode inputMode) {
		// Set the input mode image to the new value of the input mode
		if (mInputModeImageView != null && mPainter != null) {
			mInputModeImageView
					.setImageResource((inputMode == GridInputMode.NORMAL ? mPainter
							.getNormalInputModeButton() : mPainter
							.getMaybeInputModeButton()));
		}
	}
	
	/**
	 * Called when the selected digit button has been pressed.
	 */
	private void setDigitSelected(int digit) {
		mGridPlayerView.digitSelected(digit);
		mGridPlayerView.invalidate();
		setClearAndUndoButtonVisibility(mGrid.getSelectedCell());
	}

	/**
	 * Resumes the fragment.
	 */
	private void resume() {
		setTheme();

		// Propagate current preferences to the grid.
		if (mGrid != null) {
			mGrid.setPreferences();
		}

		this.setSoundEffectsEnabled(mMathDokuPreferences
				.isPlaySoundEffectEnabled());

		if (mTimerTask == null
				|| (mTimerTask != null && mTimerTask.isCancelled())) {
			startTimer();
		}
		if (!mMathDokuPreferences.isDigitButtonsVisible()) {
			if (mTickerTape != null && mTickerTape.isCancelled()) {
				mTickerTape.show();
			}
		}
		if (mGrid.mMaybeMode) {
			mGridPlayerView.toggleInputMode();
		} else {
			this.onInputModeChanged(mGridPlayerView.getGridInputMode());
		}
	}

	/**
	 * Paused the fragment.
	 */
	private void pause() {
		stopTimer();
		if (mGrid != null) {
			mGrid.save();
		}
		if (mTickerTape != null) {
			mTickerTape.cancel();
		}
	}
}