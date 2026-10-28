package com.zulrahrotations.state;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Serialisable copy of the tracker state, stored per account through ConfigManager. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackerSnapshot
{
	/** Names of the rotations still possible, e.g. ["CRIMSON_A", "CRIMSON_B"]. */
	private List<String> candidates;
	/** Zero-based phase index. */
	private int phaseIndex;
}
