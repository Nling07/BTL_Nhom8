package com.btl.n8.Model.Entity;

import com.btl.n8.Model.Enums.ItemType;

public class Figure extends Item {
    public Figure(int id, String name, int idSeller, byte[] image){
        super(id, name, idSeller, ItemType.FIGURE, image);
    }
    public Figure(String name, int idSeller, byte[] image){
        super(name, idSeller, ItemType.FIGURE, image);
    }
    public Figure(){
        super();
    }

    @Override
    public String getDescription() {
        return "Pokemon figure collectible";
    }
}
