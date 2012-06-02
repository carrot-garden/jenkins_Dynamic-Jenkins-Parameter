package com.tekante.jenkins;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.ListBoxModel;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * String based parameter that supports picking the string from two lists of
 * values presented at build time generated from data specified in job
 * configuration for this parameter and organized such that the command for the
 * second list sees the value from the first list and can change allowed values
 * dynamically
 * 
 * @author Chris Johnson
 * @see {@link ParameterDefinition}
 */

public class DynamicParameter extends ParameterDefinition {

	private static final Logger LOG = Logger.getLogger(DynamicParameter.class
			.getName());

	static final long serialVersionUID = 4;
	public String value = "";
	public String dynamicValue = "";
	public String valueOptions;
	public String dynamicValueOptions;
	public String secondName;

	@DataBoundConstructor
	public DynamicParameter(final String name, final String description,
			final String valueOptions, final String dynamicValueOptions,
			final String secondName) {
		super(name, description);
		this.secondName = secondName;
		this.valueOptions = valueOptions;
		this.dynamicValueOptions = dynamicValueOptions;
	}

	@Extension
	public static final class DescriptorImpl extends ParameterDescriptor {

		@Override
		public String getDisplayName() {
			return "Dynamic Parameter";
		}

		private DynamicParameter getDynamicParameter(final String param) {
			final String containsJobName = getCurrentDescriptorByNameUrl();
			String jobName = null;
			try {
				jobName = java.net.URLDecoder.decode(containsJobName
						.substring(containsJobName.lastIndexOf("/") + 1),
						"UTF-8");
			} catch (final UnsupportedEncodingException e) {
				LOG.warning("Could not find parameter definition instance for parameter "
						+ param
						+ " due to encoding error in job name: "
						+ e.getMessage());
				return null;
			}

			final Job<?, ?> j = Hudson.getInstance().getItemByFullName(jobName,
					hudson.model.Job.class);
			if (j != null) {
				final ParametersDefinitionProperty pdp = j
						.getProperty(hudson.model.ParametersDefinitionProperty.class);
				final List<ParameterDefinition> pds = pdp
						.getParameterDefinitions();
				for (final ParameterDefinition pd : pds) {
					if (this.isInstance(pd)
							&& ((DynamicParameter) pd).getName()
									.equalsIgnoreCase(param)) {
						return (DynamicParameter) pd;
					}
				}
			}
			LOG.warning("Could not find parameter definition instance for parameter "
					+ param);
			return null;
		}

		public ListBoxModel doFillValueItems(@QueryParameter final String name) {
			LOG.finer("Called with param: " + name);
			final ListBoxModel m = new ListBoxModel();

			final DynamicParameter dp = this.getDynamicParameter(name);
			if (dp != null) {
				for (final String s : dp.valueOptions.split("\\r?\\n")) {
					m.add(s);
				}
			}
			return m;
		}

		public ListBoxModel doFillDynamicValueItems(
				@QueryParameter final String name,
				@QueryParameter final String value) {
			final ListBoxModel m = new ListBoxModel();

			final DynamicParameter dp = this.getDynamicParameter(name);
			if (dp != null) {
				for (final String s : dp.dynamicValueOptions.split("\\r?\\n")) {
					if (s.indexOf(value) == 0) {
						final String[] str = s.split(":");
						m.add(str[1]);
					}
				}
			}
			return m;
		}
	}

	@Override
	public ParameterValue createValue(final StaplerRequest req,
			final JSONObject jo) {

		final DynamicParameterValue value = req.bindJSON(
				DynamicParameterValue.class, jo);

		return value;

	}

	@Override
	public ParameterValue createValue(final StaplerRequest req) {

		final String[] value = req.getParameterValues(getName());

		final String[] dynamicValue = req.getParameterValues(this.secondName);

		LOG.warning(getName() + ": " + value[0] + "\n");

		LOG.warning(this.secondName + ": " + dynamicValue[0] + "\n");

		return new DynamicParameterValue(getName(), value[0], this.secondName,
				dynamicValue[0]);

	}

}
