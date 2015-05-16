package org.n52.sos.ioos;

import java.util.Collections;
import java.util.Set;

import org.n52.sos.config.SettingDefinition;
import org.n52.sos.config.SettingDefinitionGroup;
import org.n52.sos.config.SettingDefinitionProvider;
import org.n52.sos.config.settings.StringSettingDefinition;

import com.google.common.collect.ImmutableSet;

public class IoosSettings implements SettingDefinitionProvider {
    public static final SettingDefinitionGroup GROUP = new SettingDefinitionGroup()
    		.setTitle("IOOS").setOrder(100);

    public static final String DISCLAIMER = "ioos.disclaimer";
    
    private static final Set<SettingDefinition<?, ?>> DEFINITIONS = ImmutableSet.<SettingDefinition<?,?>>of(        
        new StringSettingDefinition()
                .setGroup(GROUP)
                .setOrder(ORDER_1)
                .setKey(DISCLAIMER)
                .setDefaultValue("Data provided without any guarantee of accuracy."
                		+ " Provider assumes no liability whatsoever. USE AT YOUR OWN RISK.")
                .setOptional(true)
                .setTitle("Disclaimer")
                .setDescription("Disclaimer text displayed to users in GetObservation responses."));

    @Override
    public Set<SettingDefinition<?, ?>> getSettingDefinitions() {
        return Collections.unmodifiableSet(DEFINITIONS);
    }
}
