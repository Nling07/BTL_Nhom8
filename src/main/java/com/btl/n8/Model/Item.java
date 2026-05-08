package com.btl.n8.Model;

public abstract class Item {
    protected int id;
    protected String name;
    protected int sellerId;
    protected ItemType type;
    protected byte[] image;   // thêm thuộc tính ảnh

    public Item() {}

    public Item(int id, String name, int idSeller, ItemType type, byte[] image) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.sellerId = idSeller;
        this.image = image;
    }

    public Item(String name, int idSeller, ItemType type, byte[] image) {
        this.name = name;
        this.type = type;
        this.sellerId = idSeller;
        this.image = image;
    }

    // Setter
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(ItemType type) { this.type = type; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setImage(byte[] image) { this.image = image; }

    // Getter
    public int getId() { return id; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getSellerId() { return sellerId; }
    public byte[] getImage() { return image; }
}
