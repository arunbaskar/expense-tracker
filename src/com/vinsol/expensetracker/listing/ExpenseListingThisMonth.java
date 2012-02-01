package com.vinsol.expensetracker.listing;

import com.vinsol.expensetracker.R;
import com.vinsol.expensetracker.helpers.DisplayDate;

public class ExpenseListingThisMonth extends TabLayoutListingAbstract {
	
	@Override
	protected boolean condition(DisplayDate mDisplayDate) {
		return mDisplayDate.isCurrentWeek() || mDisplayDate.isCurrentMonth();
	}
	
	@Override
	protected void setType() {
		type = R.string.sublist_thismonth;
	}
}
