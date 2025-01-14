package java.awt;

import java.awt.geom.Dimension2D;
import java.beans.Transient;
import java.io.Serializable;

public class Dimension extends Dimension2D implements Serializable {
    public int width;
    public int height;
    private static final long serialVersionUID = 4723952579491349524L;

    private static native void initIDs();

    public Dimension() {
        this(0, 0);
    }

    public Dimension(Dimension d) {
        this(d.width, d.height);
    }

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return (double) this.width;
    }

    public double getHeight() {
        return (double) this.height;
    }

    public void setSize(double width, double height) {
        this.width = (int) Math.ceil(width);
        this.height = (int) Math.ceil(height);
    }

    @Transient
    public Dimension getSize() {
        return new Dimension(this.width, this.height);
    }

    public void setSize(Dimension d) {
        this.setSize(d.width, d.height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Dimension)) {
            return false;
        } else {
            Dimension d = (Dimension) obj;
            return this.width == d.width && this.height == d.height;
        }
    }

    public int hashCode() {
        int sum = this.width + this.height;
        return sum * (sum + 1) / 2 + this.width;
    }

    public String toString() {
        String var10000 = this.getClass().getName();
        return var10000 + "[width=" + this.width + ",height=" + this.height + "]";
    }

    static {
//        Toolkit.loadLibraries();
//        if (!GraphicsEnvironment.isHeadless()) {
//            initIDs();
//        }
    }
}
