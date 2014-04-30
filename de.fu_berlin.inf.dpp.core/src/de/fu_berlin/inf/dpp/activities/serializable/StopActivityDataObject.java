package de.fu_berlin.inf.dpp.activities.serializable;

import org.apache.commons.lang.ObjectUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.activities.business.StopActivity;
import de.fu_berlin.inf.dpp.activities.business.StopActivity.State;
import de.fu_berlin.inf.dpp.activities.business.StopActivity.Type;
import de.fu_berlin.inf.dpp.filesystem.IPathFactory;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A StopActivityDataObject is used for signaling to a user that he should be
 * stopped or started (meaning that no more activityDataObjects should be
 * generated by this user).
 */
@XStreamAlias("stopActivity")
public class StopActivityDataObject extends AbstractActivityDataObject {

    @XStreamAsAttribute
    protected User initiator;

    // the user who has to be locked / unlocked
    @XStreamAsAttribute
    protected User affected;

    @XStreamAsAttribute
    protected Type type;

    @XStreamAsAttribute
    protected State state;

    // a stop activityDataObject has a unique id
    @XStreamAsAttribute
    protected String stopActivityID;

    public StopActivityDataObject(User source, User initiator, User affected,
        Type type, State state, String stopActivityID) {

        super(source);

        this.initiator = initiator;
        this.affected = affected;
        this.state = state;
        this.type = type;
        this.stopActivityID = stopActivityID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ObjectUtils.hashCode(initiator);
        result = prime * result + ObjectUtils.hashCode(state);
        result = prime * result + ObjectUtils.hashCode(stopActivityID);
        result = prime * result + ObjectUtils.hashCode(type);
        result = prime * result + ObjectUtils.hashCode(affected);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof StopActivityDataObject))
            return false;

        StopActivityDataObject other = (StopActivityDataObject) obj;

        if (!ObjectUtils.equals(this.state, other.state))
            return false;
        if (!ObjectUtils.equals(this.type, other.type))
            return false;
        if (!ObjectUtils.equals(this.stopActivityID, other.stopActivityID))
            return false;
        if (!ObjectUtils.equals(this.initiator, other.initiator))
            return false;
        if (!ObjectUtils.equals(this.affected, other.affected))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "StopActivityDO(id: " + stopActivityID + ", type: " + type
            + ", state: " + state + ", initiator: " + initiator
            + ", affected user: " + affected + ", src: " + getSource() + ")";
    }

    @Override
    public IActivity getActivity(ISarosSession sarosSession,
        IPathFactory pathFactory) {
        return new StopActivity(getSource(), initiator, affected, type, state,
            stopActivityID);
    }
}