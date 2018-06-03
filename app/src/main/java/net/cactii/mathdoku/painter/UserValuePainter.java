package net.cactii.mathdoku.painter;

import net.cactii.mathdoku.painter.Painter.DigitPainterMode;
import net.cactii.mathdoku.painter.Painter.GridTheme;
import android.graphics.Paint;

public class UserValuePainter extends DigitPainter {

	/**
	 * Creates a new instance of {@link UserValuePainter}.
	 * 
	 * @param painter
	 *            The global container for all painters.
	 */
	public UserValuePainter(Painter painter) {
		super(painter);
		mTextPaintNormalInputMode = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaintMaybeInputMode = new Paint(Paint.ANTI_ALIAS_FLAG);
	}

	@Override
	public void setTheme(GridTheme theme) {
		mTextPaintNormalInputMode.setColor(mPainter
				.getHighlightedTextColorNormalInputMode());
		mTextPaintMaybeInputMode.setColor(mPainter.getDefaultTextColor());

		mTextPaintNormalInputMode.setTypeface(mPainter.getTypeface());

		mTextPaintMaybeInputMode.setTypeface(mPainter.getTypeface());
		
	}

	@Override
	protected void setCellSize(float size) {
		// 1/3 of cell is used for the cage results. Remaining space can be used
		// to display the user value.
		int bottomMargin = (int) (size * 0.2);
		int userValueTextSize = (int) (size * 3/4);

		mTextPaintNormalInputMode.setTextSize(userValueTextSize);
		mTextPaintMaybeInputMode.setTextSize(userValueTextSize);

		// Compute the offsets at which the user value will be displayed within
		// the cell
		mLeftOffset = size / 2 - userValueTextSize / 4;

		mBottomOffset = size - bottomMargin;
	}

	@Override
	public Paint getTextPaintNormalInputMode() {
		return (mDigitPainterMode == DigitPainterMode.INPUT_MODE_BASED ? mTextPaintNormalInputMode : mTextPaintMaybeInputMode);
	}
}
