package top.nowandfuture.mod.imagesign.caches;

public class GIFParam implements IParam{
    private int frameWidth, frameHeight;
    private int disposalMethod = 0;
    private int userInputFlag = 0;
    private int transparencyFlag = 0;
    private int delay;
    private int transparentColor;
    private int leftPosition;
    private int topPosition;

    private GIFParam() {

    }

    private GIFParam(int frameWidth, int frameHeight, int disposalMethod, int userInputFlag, int transparencyFlag, int delay, int transparentColor, int leftPosition, int topPosition) {
        this.disposalMethod = disposalMethod;
        this.userInputFlag = userInputFlag;
        this.transparencyFlag = transparencyFlag;
        this.delay = delay;
        this.transparentColor = transparentColor;
        this.leftPosition = leftPosition;
        this.topPosition = topPosition;
        this.frameHeight = frameHeight;
        this.frameWidth = frameWidth;
    }

    public int getTransparencyFlag() {
        return transparencyFlag;
    }

    public void setTransparencyFlag(int transparencyFlag) {
        this.transparencyFlag = transparencyFlag;
    }

    public int getUserInputFlag() {
        return userInputFlag;
    }

    public void setUserInputFlag(int userInputFlag) {
        this.userInputFlag = userInputFlag;
    }

    public int getDisposalMethod() {
        return disposalMethod;
    }

    public void setDisposalMethod(int disposalMethod) {
        this.disposalMethod = disposalMethod;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getTransparentColor() {
        return transparentColor;
    }

    public void setTransparentColor(int transparentColor) {
        this.transparentColor = transparentColor;
    }

    public int getLeftPosition() {
        return leftPosition;
    }

    public void setLeftPosition(int leftPosition) {
        this.leftPosition = leftPosition;
    }

    public int getTopPosition() {
        return topPosition;
    }

    public void setTopPosition(int topPosition) {
        this.topPosition = topPosition;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public static class Builder {
        private int leftPosition;
        private int topPosition;
        private int frameWidth;
        private int frameHeight;
        private int delay;
        private int disposalMethod;
        private int userInputFlag;
        private int transparencyFlag;
        private int transparentColor;

        Builder(int frameWidth, int frameHeight, int leftPosition, int topPosition, int delay, int transparentColor) {
            this.delay = delay;
            this.leftPosition = leftPosition;
            this.topPosition = topPosition;
            this.transparentColor = transparentColor;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
        }

        public static Builder newBuild(int frameWidth, int frameHeight, int leftPosition, int topPosition, int delay, int transparentColor) {
            return new Builder(frameWidth, frameHeight, leftPosition, topPosition, delay, transparentColor);
        }

        public Builder setTransparentColor(int transparentColor) {
            this.transparentColor = transparentColor;
            return this;
        }

        public Builder setTransparencyFlag(int transparencyFlag) {
            this.transparencyFlag = transparencyFlag;
            return this;
        }

        public Builder setUserInputFlag(int userInputFlag) {
            this.userInputFlag = userInputFlag;
            return this;
        }

        public Builder setDisposalMethod(int disposalMethod) {
            this.disposalMethod = disposalMethod;
            return this;
        }

        public Builder setDelay(int delay) {
            this.delay = delay;
            return this;
        }

        public Builder setLeftPosition(int leftPosition) {
            this.leftPosition = leftPosition;
            return this;
        }

        public Builder setTopPosition(int topPosition) {
            this.topPosition = topPosition;
            return this;
        }
        
        public GIFParam build(){
            return new GIFParam(frameWidth, frameHeight, disposalMethod, userInputFlag, transparencyFlag, delay, transparentColor, leftPosition, topPosition);
        }

        public Builder setFrameHeight(int frameHeight) {
            this.frameHeight = frameHeight;
            return this;
        }

        public Builder setFrameWidth(int frameWidth) {
            this.frameWidth = frameWidth;
            return this;
        }
    }
}
