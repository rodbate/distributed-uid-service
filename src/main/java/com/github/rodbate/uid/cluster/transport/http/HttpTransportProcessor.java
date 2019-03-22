package com.github.rodbate.uid.cluster.transport.http;

import com.github.rodbate.uid.cluster.transport.TransportProcessor;
import org.reactivestreams.Publisher;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 10:09
 */
public interface HttpTransportProcessor extends TransportProcessor<HttpTransportContext, Publisher<Void>> {


}
