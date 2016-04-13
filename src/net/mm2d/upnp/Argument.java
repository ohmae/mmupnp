/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Argument {
    public static class Builder {
        private Action mAction;
        private String mName;
        private boolean mInputDirection;
        private String mRelatedStateVariableName;
        private StateVariable mRelatedStateVariable;

        public Builder() {
        }

        public void setAction(Action action) {
            mAction = action;
        }

        public void setName(String name) {
            mName = name;
        }

        public void setDirection(String direction) {
            mInputDirection = "in".equalsIgnoreCase(direction);
        }

        public void setRelatedStateVariableName(String name) {
            mRelatedStateVariableName = name;
        }

        public void setRelatedStateVariable(StateVariable variable) {
            mRelatedStateVariable = variable;
        }

        public String getRelatedStateVariableName() {
            return mRelatedStateVariableName;
        }

        public Argument build() {
            return new Argument(this);
        }
    }

    private final Action mAction;
    private final String mName;
    private final boolean mInputDirection;
    private final StateVariable mRelatedStateVariable;

    private Argument(Builder builder) {
        mAction = builder.mAction;
        mName = builder.mName;
        mInputDirection = builder.mInputDirection;
        mRelatedStateVariable = builder.mRelatedStateVariable;
    }

    public Action getAction() {
        return mAction;
    }

    public String getName() {
        return mName;
    }

    public boolean isInputDirection() {
        return mInputDirection;
    }

    public StateVariable getRelatedStateVariable() {
        return mRelatedStateVariable;
    }
}
