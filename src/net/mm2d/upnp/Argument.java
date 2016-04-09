/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Argument {
    private final Action mAction;
    private String mName;
    private boolean mInputDirection;
    private String mRelatedStateVariableName;
    private StateVariable mRelatedStateVariable;

    public Argument(Action action) {
        mAction = action;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isInputDirection() {
        return mInputDirection;
    }

    public void setDirection(String direction) {
        mInputDirection = "in".equalsIgnoreCase(direction);
    }

    public String getRelatedStateVariableName() {
        return mRelatedStateVariableName;
    }

    public void setRelatedStateVariableName(String relatedStateVariableName) {
        mRelatedStateVariableName = relatedStateVariableName;
    }

    public StateVariable getRelatedStateVariable() {
        return mRelatedStateVariable;
    }

    public void setRelatedStateVariable(StateVariable relatedStateVariable) {
        mRelatedStateVariable = relatedStateVariable;
    }

    public Action getAction() {
        return mAction;
    }
}
