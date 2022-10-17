package hudson.plugins.rootspace_monitor;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.node_monitors.AbstractDiskSpaceMonitor;
import hudson.node_monitors.DiskSpaceMonitorDescriptor;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;


/**
 * Monitors the disk space of "/".
 *
 * @author bright.ma
 */
public class RootSpaceMonitor extends AbstractDiskSpaceMonitor {
    @DataBoundConstructor
    public RootSpaceMonitor(String freeSpaceThreshold) throws ParseException {
        super(freeSpaceThreshold);
    }

    public RootSpaceMonitor() {}

    public DiskSpaceMonitorDescriptor.DiskSpace getFreeSpace(Computer c) {
        return DESCRIPTOR.get(c);
    }

    @Override
    public String getColumnCaption() {
        // Hide this column from non-admins
        return Jenkins.get().hasPermission(Jenkins.ADMINISTER) ? super.getColumnCaption() : null;
    }

    /**
     * @deprecated as of 2.0
     *      Use injection
     */
    @Deprecated
    public static /*almost final*/ DiskSpaceMonitorDescriptor DESCRIPTOR;

    @Extension @Symbol("rootSpace")
    public static class DescriptorImpl extends DiskSpaceMonitorDescriptor {
        public DescriptorImpl() {
            DESCRIPTOR = this;
        }

        @Override
        public String getDisplayName() {
            return Messages.RootSpaceMonitor_DisplayName();
        }

        @Override
        protected Callable<DiskSpace,IOException> createCallable(Computer c) {
            Node node = c.getNode();
            if (node == null) return null;

            FilePath p = node.getRootPath();
            if(p==null) return null;

            return p.asCallableWith(new GetRootSpace());
        }
    }

    /**
     * @deprecated as of 2.0
     */
    @Deprecated
    public static DiskSpaceMonitorDescriptor install() {
        return DESCRIPTOR;
    }

    protected static final class GetRootSpace extends MasterToSlaveFileCallable<DiskSpaceMonitorDescriptor.DiskSpace> {
        @Override
        public DiskSpaceMonitorDescriptor.DiskSpace invoke(File f, VirtualChannel channel) throws IOException {
            // if the disk is really filled up we can't even create a single file,
            // so calling File.createTempFile and figuring out the directory won't reliably work.
            f = new File("/");
            long s = f.getUsableSpace();
            if(s<=0)    return null;
            return new DiskSpaceMonitorDescriptor.DiskSpace(f.getCanonicalPath(), s);
        }
        private static final long serialVersionUID = 1L;
    }
}
