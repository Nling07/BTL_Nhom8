package com.btl.n8.Service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.Card;
import com.btl.n8.Model.Item;
import com.btl.n8.Model.ItemType;

import java.util.List;
import java.util.stream.Collectors;

public class CardService {
    private final ItemDAO itemDAO;

    public CardService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public boolean addCard(Card card) {
        return itemDAO.insert(card);
    }

    public Card getCardById(int id) {
        Item item = itemDAO.findById(id);
        if (item != null && item.getType() == ItemType.CARD) {
            return (Card) item;
        }
        return null;
    }

    public List<Card> getAllCards() {
        return itemDAO.findAll().stream()
                .filter(i -> i.getType() == ItemType.CARD)
                .map(i -> (Card) i)
                .collect(Collectors.toList());
    }

    public List<Card> getCardsBySeller(int sellerId) {
        return itemDAO.findBySeller(sellerId).stream()
                .filter(i -> i.getType() == ItemType.CARD)
                .map(i -> (Card) i)
                .collect(Collectors.toList());
    }
}
