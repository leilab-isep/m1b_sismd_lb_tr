package WithoutThreadPool;

class Page_WithoutThreadPool {
    private String title;
    private String text;

    public Page_WithoutThreadPool(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}
