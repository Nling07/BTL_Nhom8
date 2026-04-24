package com.btl.n8.service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.Poster;
import com.btl.n8.Model.Item;
import com.btl.n8.Model.ItemType;

import java.util.List;
import java.util.stream.Collectors;

public class PosterService {
    private final ItemDAO itemDAO;

    public PosterService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public boolean addPoster(Poster poster) {
        return itemDAO.insert(poster);
    }

    public Poster getPosterById(int id) {
        Item item = itemDAO.findById(id);
        if (item != null && item.getType() == ItemType.POSTER) {
            return (Poster) item;
        }
        return null;
    }

    public List<Poster> getAllPosters() {
        return itemDAO.findAll().stream()
                .filter(i -> i.getType() == ItemType.POSTER)
                .map(i -> (Poster) i)
                .collect(Collectors.toList());
    }

    public List<Poster> getPostersBySeller(int sellerId) {
        return itemDAO.findBySeller(sellerId).stream()
                .filter(i -> i.getType() == ItemType.POSTER)
                .map(i -> (Poster) i)
                .collect(Collectors.toList());
    }
}
