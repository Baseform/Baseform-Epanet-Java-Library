package org.addition.epanetold.Types;

/* PIPE SEGMENT record used */
/*   for WQ routing         */
//Sseg
public class PipeSegment {

   double  volume;
   double  quality;

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }
}
