package com.github.engatec.vdl.core.youtubedl.processbuilder;

import java.io.IOException;
import java.util.List;

import com.github.engatec.vdl.core.youtubedl.YoutubeDlCommandBuilder;
import com.github.engatec.vdl.core.youtubedl.YoutubeDlCommandHelper;
import org.apache.commons.lang3.StringUtils;

public class DownloadableInfoFetchProcessBuilder implements YoutubeDlProcessBuilder {

    private final String url;

    public DownloadableInfoFetchProcessBuilder(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url must not be blank");
        }
        this.url = url;
    }

    @Override
    public List<String> buildCommand() {
        YoutubeDlCommandBuilder commandBuilder = YoutubeDlCommandBuilder.newInstance()
                .dumpJson()
                .ignoreErrors()
                .noCheckCertificate()
                .flatPlaylist();

        YoutubeDlCommandHelper.setNetworkOptions(commandBuilder);
        YoutubeDlCommandHelper.setAuthenticationOptions(commandBuilder);

        return commandBuilder
                .url(url)
                .buildAsList();
    }

    @Override
    public Process buildProcess(List<String> command) throws IOException {
        return new ProcessBuilder(command).start();
    }
}
