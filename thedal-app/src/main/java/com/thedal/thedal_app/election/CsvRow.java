package com.thedal.thedal_app.election;

import java.util.List;

public class CsvRow {

    private List<String> cells;

    public CsvRow(List<String> cells) {
        this.cells = cells;
    }

    public String getCell(int index) {
        if (index >= 0 && index < cells.size()) {
            return cells.get(index);
        }
        return null; // Handle invalid index
    }
}


