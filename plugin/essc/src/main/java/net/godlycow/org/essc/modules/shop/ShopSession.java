package net.godlycow.org.essc.modules.shop;

public class ShopSession {
    private final String categoryId;
    private final int page;

    public ShopSession(String categoryId, int page) {
        this.categoryId = categoryId;
        this.page = page;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getPage() {
        return page;
    }
}