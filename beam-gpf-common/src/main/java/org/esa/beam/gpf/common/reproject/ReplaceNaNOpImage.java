/*
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

final class ReplaceNaNOpImage extends PointOpImage {

    private final double replacementValue;
    
    public ReplaceNaNOpImage(RenderedImage source, double value) {
        super(source, null, null, false);
        this.replacementValue = value;
    }

    @Override
    protected void computeRect(Raster sources[], WritableRaster dest, Rectangle destRect) {
        RasterFormatTag formatTags[] = getFormatTags();
        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        switch (d.getDataType()) {
            case 4: // '\004'
                computeRectFloat(s, d, (float) replacementValue);
                break;

            case 5: // '\005'
                computeRectDouble(s, d, replacementValue);
                break;
        }
        d.copyDataToRaster();
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor dst, float rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sBandOffsets[] = src.getBandOffsets();
        float sData[][] = src.getFloatDataArrays();
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffsets[] = dst.getBandOffsets();
        float dData[][] = dst.getFloatDataArrays();
        float s[] = sData[0];
        float d[] = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                float sourceValue = s[sPixelOffset];
                if (Float.isNaN(sourceValue)) {
                    d[dPixelOffset] = rValue;
                } else {
                    d[dPixelOffset] = sourceValue;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor dst, double rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int sBandOffsets[] = src.getBandOffsets();
        double sData[][] = src.getDoubleDataArrays();
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffsets[] = dst.getBandOffsets();
        double dData[][] = dst.getDoubleDataArrays();
        double s[] = sData[0];
        double d[] = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                double sourceValue = s[sPixelOffset];
                if (Double.isNaN(sourceValue)) {
                    d[dPixelOffset] = rValue;
                } else {
                    d[dPixelOffset] = sourceValue;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }
}
