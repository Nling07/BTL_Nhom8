package com.btl.n8.Service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.Figure;
import com.btl.n8.Model.Item;
import com.btl.n8.Model.ItemType;

import java.util.List;
import java.util.stream.Collectors;

public class FigureService {
    private final ItemDAO itemDAO;

    public FigureService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public boolean addFigure(Figure figure) {
        return itemDAO.insert(figure);
    }

    public Figure getFigureById(int id) {
        Item item = itemDAO.findById(id);
        if (item != null && item.getType() == ItemType.FIGURE) {
            return (Figure) item;
        }
        return null;
    }

    public List<Figure> getAllFigures() {
        return itemDAO.findAll().stream()
                .filter(i -> i.getType() == ItemType.FIGURE)
                .map(i -> (Figure) i)
                .collect(Collectors.toList());
    }

    public List<Figure> getFiguresBySeller(int sellerId) {
        return itemDAO.findBySeller(sellerId).stream()
                .filter(i -> i.getType() == ItemType.FIGURE)
                .map(i -> (Figure) i)
                .collect(Collectors.toList());
    }
}
