package com.btl.n8.Model;

import java.math.BigDecimal;

public class Figure extends Item{
    public Figure(int id, String name, int idSeller){
        super(id, name, idSeller, ItemType.FIGURE);
    }
    public Figure(){
        super();
    }
}
