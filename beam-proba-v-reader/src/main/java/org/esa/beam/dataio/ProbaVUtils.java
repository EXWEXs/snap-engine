package org.esa.beam.dataio;

import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.h5.H5Datatype;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Proba-V utility methods
 *
 * @author olafd
 */
public class ProbaVUtils {

    // todo: implement tests!!!

    public static String getAttributeValue(Attribute attribute) {
        String result = "";
        switch (attribute.getType().getDatatypeClass()) {
            case Datatype.CLASS_INTEGER:
                int[] ivals = (int[]) attribute.getValue();
                for (int ival : ivals) {
                    result = result.concat(Integer.toString(ival) + "  ");
                }
                break;
            case Datatype.CLASS_FLOAT:
                float[] fvals = (float[]) attribute.getValue();
                for (float fval : fvals) {
                    result = result.concat(Float.toString(fval) + "  ");
                }
                break;
            case Datatype.CLASS_STRING:
                String[] svals = (String[]) attribute.getValue();
                for (String sval : svals) {
                    result = result.concat(sval + "  ");
                }
                break;
            default:
                break;
        }

        return result;
    }

    public static String getDescriptionFromAttributes(List<Attribute> metadata) {
        String description = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("DESCRIPTION")) {
                try {
                    description = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse product description string: " +
                            e.getMessage());
                }
            }
        }
        return description;
    }

    public static String getUnitFromAttributes(List<Attribute> metadata) {
        String unit = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("UNITS")) {
                try {
                    unit = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse units string: " +
                            e.getMessage());
                }
            }
        }
        if (unit != null && unit.contains("-")) {
            return "dl";
        } else {
            return unit;
        }
    }

    public static float getNoDataValueFromAttributes(List<Attribute> metadata) {
        float noDataValue = Float.NaN;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("NO_DATA")) {
                try {
                    noDataValue = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse product noDataValue string: " +
                            e.getMessage());
                }
            }
        }
        return noDataValue / getScaleFactorFromAttributes(metadata);
    }

    public static float getScaleFactorFromAttributes(List<Attribute> metadata) {
        float scaleFactor = 1.0f;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("SCALE")) {
                try {
                    scaleFactor = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse scale factor: " + e.getMessage());
                }
            }
        }
        return 1.0f / scaleFactor;
    }

    public static float getScaleOffsetFromAttributes(List<Attribute> metadata) {
        float scaleOffset = 0.0f;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("OFFSET")) {
                try {
                    scaleOffset = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse scale offset: " + e.getMessage());
                }
            }
        }
        return scaleOffset;
    }

    public static double getGeometryCoordinateValueFromAttributes(List<Attribute> metadata, String coordinateName) {
        double coordValue = Double.NaN;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(coordinateName)) {
                try {
                    coordValue = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse geometry coordinate: " +
                            e.getMessage());
                }
            }
        }
        return coordValue;
    }

    public static String getGeometryCrsStringFromAttributes(List<Attribute> metadata) {
        String crsString = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("MAP_PROJECTION_WKT")) {
                try {
                    crsString = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse CRS WKT string: " +
                            e.getMessage());
                }
            }
        }
        return crsString;
    }

    public static String[] getStartEndTimeFromAttributes(List<Attribute> metadata) {
        String[] startStopTimes = new String[2];
        String startDate = "";
        String startTime = "";
        String endDate = "";
        String endTime = "";
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("OBSERVATION_START_DATE")) {
                startDate = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_START_TIME")) {
                startTime = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_END_DATE")) {
                endDate = getAttributeValue(attribute);
            } else if (attribute.getName().equals("OBSERVATION_END_TIME")) {
                endTime = getAttributeValue(attribute);
            }
        }

        // format is 'yyyy-mm-dd hh:mm:ss'
        startStopTimes[0] = startDate + " " + startTime;
        startStopTimes[1] = endDate + " " + endTime;
        return startStopTimes;
    }

    public static float[] getNdviAsFloat(Band ndviBand, byte[] ndviData) {
        float[] ndviFloatData = new float[ndviData.length];

        // apply scaling to physical values manually
        for (int i = 0; i < ndviFloatData.length; i++) {
            float ndviTmp = (float) ndviData[i];
            ndviTmp += 256.0f;
            if (ndviTmp >= 256.0f) {
                ndviTmp -= 256.0f;
            }
            if (ndviTmp == 255.0f) {
                ndviFloatData[i] = ProbaVConstants.NDVI_NO_DATA_VALUE;
            } else {
                ndviFloatData[i] = (float) ((ndviTmp - ndviBand.getScalingOffset()) * ndviBand.getScalingFactor());
            }
        }

        // set scaling to neutral values:
        ndviBand.setScalingFactor(1.0);
        ndviBand.setScalingOffset(0.0);

        return ndviFloatData;
    }

    public static ProductData getNdviProductDataAsFloats(Band ndviBand, ProductData ndviRasterData) {
        final byte[] ndviRasterDataElems = (byte[]) ndviRasterData.getElems();
        final float[] ndviFloatRasterDataElems = getNdviAsFloat(ndviBand, ndviRasterDataElems);
        return ProductData.createInstance(ndviFloatRasterDataElems);
    }

    public static boolean isGeometryBand(String bandName) {
        return bandName.equals("SZA") || bandName.startsWith("VZA") || bandName.equals("SAA") || bandName.startsWith("VAA");
    }

    public static void addSynthesisQualityMasks(Product probavProduct) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLEAR_FLAG_NAME,
                ProbaVConstants.SM_CLEAR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                ProbaVConstants.SM_UNDEFINED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                ProbaVConstants.SM_SNOWICE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_LAND_FLAG_NAME,
                ProbaVConstants.SM_LAND_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[6], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[7], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                ProbaVConstants.SM_GOOD_RED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[8], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[9], 0.5f);
    }

    public static void addSynthesisQualityFlags(FlagCoding probavSmFlagCoding) {
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLEAR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLEAR_BIT_INDEX),
                                   ProbaVConstants.SM_CLEAR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_UNDEFINED_BIT_INDEX),
                                   ProbaVConstants.SM_UNDEFINED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_SNOWICE_INDEX),
                                   ProbaVConstants.SM_SNOWICE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_LAND_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_LAND_BIT_INDEX),
                                   ProbaVConstants.SM_LAND_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_SWIR_INDEX),
                                   ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_NIR_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_RED_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_RED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR);
    }

    public static void addL1cQualityMasks(Product probavProduct, String sourceQualityBandName, String targetQualityFlagBandName) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_CORRECT_FLAG_NAME,
                ProbaVConstants.Q_CORRECT_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_MISSING_FLAG_NAME,
                ProbaVConstants.Q_MISSING_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_WAS_SATURATED_FLAG_NAME,
                ProbaVConstants.Q_WAS_SATURATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_SATURATED_FLAG_NAME,
                ProbaVConstants.Q_BECAME_SATURATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_NAME,
                ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_INTERPOLATED_FLAG_NAME,
                ProbaVConstants.Q_INTERPOLATED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, targetQualityFlagBandName,
                sourceQualityBandName + "_" + ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_NAME,
                ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[6], 0.5f);
    }

    public static void addL1cQualityFlags(FlagCoding probavSmFlagCoding, String sourceQualityBandName) {
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_CORRECT_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_CORRECT_BIT_INDEX),
                                   ProbaVConstants.Q_CORRECT_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_MISSING_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_MISSING_BIT_INDEX),
                                   ProbaVConstants.Q_MISSING_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_WAS_SATURATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_WAS_SATURATED_BIT_INDEX),
                                   ProbaVConstants.Q_WAS_SATURATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_SATURATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BECAME_SATURATED_INDEX),
                                   ProbaVConstants.Q_BECAME_SATURATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BECAME_NEGATIVE_BIT_INDEX),
                                   ProbaVConstants.Q_BECAME_NEGATIVE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_INTERPOLATED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_INTERPOLATED_BIT_INDEX),
                                   ProbaVConstants.Q_INTERPOLATED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(sourceQualityBandName + "_" + ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.Q_BORDER_COMPRESSED_INDEX),
                                   ProbaVConstants.Q_BORDER_COMPRESSED_FLAG_DESCR);
    }



    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                                              description, width, height,
                                              bandName + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }

    public static int getDatatypeForH5Dread(int datatypeClass) {
        switch (datatypeClass) {
            case H5Datatype.CLASS_BITFIELD:
                return HDF5Constants.H5T_NATIVE_UINT8;
            case H5Datatype.CLASS_CHAR:
                return HDF5Constants.H5T_NATIVE_UINT8;
            case H5Datatype.CLASS_FLOAT:
                return HDF5Constants.H5T_NATIVE_FLOAT;
            case H5Datatype.CLASS_INTEGER:
                return HDF5Constants.H5T_NATIVE_INT16;
            default:
                break;
        }
        return -1;
    }

    public static ProductData getDataBufferForH5Dread(int datatypeClass, int width, int height) {
        switch (datatypeClass) {
            case H5Datatype.CLASS_CHAR:
//                return ProductData.createUnsignedInstance(new byte[width * height]);
                return ProductData.createInstance(new byte[width * height]);
            case H5Datatype.CLASS_FLOAT:
                return ProductData.createInstance(new float[width*height]);
            case H5Datatype.CLASS_INTEGER:
                return ProductData.createInstance(new short[width*height]);
            default:
                break;
        }
        return null;
    }

}
