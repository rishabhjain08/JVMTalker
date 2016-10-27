package api;

import breakpoints.ProfilerLineBreakpoint;

/**
 * Created by rishajai on 10/18/16.
 */
public interface ProfilerLineBreakpointCallback {

    public void breakpointHit(ProfilerLineBreakpoint breakpoint);
}
