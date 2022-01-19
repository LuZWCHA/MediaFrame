package top.nowandfuture.mod.imagesign.utils;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class ParamsParser {

    public static class Params{
        public double offsetX, offsetY, offsetZ, width, height;
        public boolean doOffset;
        public int combineLight;

        private Params(double offsetX, double offsetY, double offsetZ, double width, double height, int combineLight, boolean doOffset){
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.width = width;
            this.height = height;
            this.doOffset = doOffset;
            this.combineLight = combineLight;
        }


        public static Params create(double offsetX, double offsetY, double offsetZ, double width, double height, int combineLight, boolean doOffset){
            return new Params(offsetX, offsetY, offsetZ, width, height, combineLight, doOffset);
        }
    }

    public static Params parse(String []lines, int combinedLightIn){
        String pram = lines[2];
        String[] res = pram.split(",");
        DoubleList pars = new DoubleArrayList();
        String brightness = lines[3];
        int light = 0;
        try {
            light = Integer.parseInt(brightness);

        } catch (NumberFormatException ignored) {

        }
        try {
            for (String re : res) {
                double data = Double.parseDouble(re);
                pars.add(data);
            }
        } catch (NumberFormatException ignored) {

        }

        double width = 1, height = 1;
        boolean doOffset = false;
        double offsetX = 0D, offsetY = 0D, offsetZ = 0D;

        if (pars.size() == 2) {
            if (pars.getDouble(0) > 0) {
                width = pars.getDouble(0);
            }

            if (pars.getDouble(1) > 0) {
                height = pars.getDouble(1);
            }
        }

        if (pars.size() == 3) {
            doOffset = true;
            offsetX = pars.getDouble(0);
            offsetY = pars.getDouble(1);
            offsetZ = pars.getDouble(2);
        }

        if (pars.size() == 5) {
            doOffset = true;
            if (pars.getDouble(0) > 0) {
                width = pars.getDouble(0);
            }

            if (pars.getDouble(1) > 0) {
                height = pars.getDouble(1);
            }
            offsetX = pars.getDouble(2);
            offsetY = pars.getDouble(3);
            offsetZ = pars.getDouble(4);
        }

        int[] lights = RenderHelper.decodeCombineLight(combinedLightIn);
        combinedLightIn = RenderHelper.getCombineLight(lights[0], lights[1], light);

        return Params.create(offsetX, offsetY, offsetZ, width, height, combinedLightIn, doOffset);
    }
}
