package java.awt.geom;

public abstract class Dimension2D implements Cloneable {
    protected Dimension2D() {
    }

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setSize(double var1, double var3);

    public void setSize(Dimension2D d) {
        this.setSize(d.getWidth(), d.getHeight());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException var2) {
            CloneNotSupportedException e = var2;
            throw new Error(e);
        }
    }
}
