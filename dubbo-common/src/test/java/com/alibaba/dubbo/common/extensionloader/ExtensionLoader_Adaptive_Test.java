/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.extensionloader;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt;
import com.alibaba.dubbo.common.extensionloader.adaptive.impl.HasAdaptiveExt_ManualAdaptive;
import com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt;
import com.alibaba.dubbo.common.extensionloader.ext2.Ext2;
import com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder;
import com.alibaba.dubbo.common.extensionloader.ext3.UseProtocolKeyExt;
import com.alibaba.dubbo.common.extensionloader.ext4.NoUrlParamExt;
import com.alibaba.dubbo.common.extensionloader.ext5.NoAdaptiveMethodExt;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.impl.Ext6Impl2;
import com.alibaba.dubbo.common.utils.LogUtil;

import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * 对@Adaptive注解的测试
 *
 * 理解了这个测试类中的东西，那么 @Adaptive 也就明白了。!!!!!!!!!!
 *
 */
public class ExtensionLoader_Adaptive_Test {

    /**
     * 获取对应的扩展类实现
     * TODO 如果给 HasAdaptiveExtImpl1实现类也加上@Adaptive 注解 ，那么还是会加载HasAdaptiveExt_ManualAdaptive 不知道为啥 .待研究。
     *
     * 为什么有些实现类上会标注©Adaptive呢？放在实现类上，主要是为了直接固定对
     * 应的实现而不需要动态生成代码实现，就像策略模式直接确定实现类。在代码中的实现方式是： ExtensionLoader中会缓存两个与©Adaptive有关的对象，
     * 一个缓存在cachedAdaptiveClass中， 即Adaptive具体实现类的Class类型；另外一个缓存在cachedAdaptivelnstance中，即Class
     * 的具体实例化对象。在扩展点初始化时，如果发现实现类有@Adaptive注解，则直接赋值给
     * cachedAdaptiveClass ,后续实例化类的时候，就不会再动态生成代码，直接实例化
     * cachedAdaptiveClass,并把实例缓存到cachedAdaptivelnstance中。如果注解在接口方法上， 则会根据参数，动态获得扩展点的实现，
     * 会生成Adaptive类，再缓存到 cachedAdaptivelnstance 中，在调用接口的某个方法时候，将会从URL（总线上获取 param对应的value,然后根据value获取对应的实现类
     * 去完成真正的调用实现逻辑）
     *
     * @throws Exception
     */
    @Test
    public void test_useAdaptiveClass() throws Exception {
        ExtensionLoader<HasAdaptiveExt> loader = ExtensionLoader.getExtensionLoader(HasAdaptiveExt.class);
        //因为已经在 HasAdaptiveExt_ManualAdaptive实现类上加了 @Adaptive注解所以不会使用字节码给 HasAdaptiveExt接口生成实现类了，
        //而是直接使用 HasAdaptiveExt_ManualAdaptive 为 HasAdaptiveExt 的默认实现。
        HasAdaptiveExt ext = loader.getAdaptiveExtension();

        assertTrue(ext instanceof HasAdaptiveExt_ManualAdaptive);
    }

    /**
     *
     * 使用默认的key 默认实现的配置在 ，@SPI配置的(impl1)
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_defaultAdaptiveKey() throws Exception {
        {
            SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

            Map<String, String> map = new HashMap<String, String>();
            URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);
            //使用默认配置 即 @SPI(impl1)中配置的 impl1
            String echo = ext.echo(url, "haha");
            assertEquals("Ext1Impl1-echo", echo);
        }

        {

            SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

            Map<String, String> map = new HashMap<String, String>();
            //指定impl2为实现
            map.put("simple.ext", "impl2");
            URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

            String echo = ext.echo(url, "haha");
            assertEquals("Ext1Impl2-echo", echo);
        }
    }

    /**
     * 自定义 @Adaptive 的 Key
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_customizeAdaptiveKey() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        //key2 已经在 SimpleExt接口的 yell方法定义 如下 :  @Adaptive({"key1", "key2"})
        map.put("key2", "impl2");
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        String echo = ext.yell(url, "haha");
        assertEquals("Ext1Impl2-yell", echo);

        url = url.addParameter("key1", "impl3"); // note: URL is value's type
        echo = ext.yell(url, "haha");
        assertEquals("Ext1Impl3-yell", echo);
    }

    /**
     *
     * 使用 protocol
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_protocolKey() throws Exception {
        UseProtocolKeyExt ext = ExtensionLoader.getExtensionLoader(UseProtocolKeyExt.class).getAdaptiveExtension();

        {
            String echo = ext.echo(URL.valueOf("1.2.3.4:20880"), "s");
            //使用 @SPI("impl1") 上的 impl1 作为扩展接口的实现。
            assertEquals("Ext3Impl1-echo", echo); // default value

            Map<String, String> map = new HashMap<String, String>();

            //因为此处在new URL对象的时候，protocol的值设置为了 impl3 所以将会选择 impl3作为实现
            URL url = new URL("impl3", "1.2.3.4", 1010, "path1", map);

            echo = ext.echo(url, "s");
            assertEquals("Ext3Impl3-echo", echo); // use 2nd key, protocol

            //调用 url.addParameter("key1", "impl2");  后将使用impl2作为扩展接口的实现
            url = url.addParameter("key1", "impl2");
            echo = ext.echo(url, "s");
            assertEquals("Ext3Impl2-echo", echo); // use 1st key, key1
        }

        {

            /**
             * 通过这个例子 可以看到使用 addParameter 和 setProtocol 都可以控制实现类的选择
             */
            Map<String, String> map = new HashMap<String, String>();
            URL url = new URL(null, "1.2.3.4", 1010, "path1", map);
            String yell = ext.yell(url, "s");
            assertEquals("Ext3Impl1-yell", yell); // default value

            url = url.addParameter("key2", "impl2"); // use 2nd key, key2 ，使用 impl2作为实现
            yell = ext.yell(url, "s");
            assertEquals("Ext3Impl2-yell", yell);

            url = url.setProtocol("impl3"); // use 1st key, protocol 使用impl3作为实现
            yell = ext.yell(url, "d");
            assertEquals("Ext3Impl3-yell", yell);
        }
    }

    /**
     * URL是null时候， 将会报错
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_UrlNpe() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("url == null", e.getMessage());
        }
    }

    /**
     * 接口某方法不添加 @Adaptive ，但是却使用  getAdaptiveExtension();调用的话，将会给出如下的异常。
     *
     * 但是如果使用  getExtension 的话，就没问题喽。可以正常获取到。
     *
     *
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoAdaptiveMethodOnInterface() throws Exception {
        try {
            NoAdaptiveMethodExt impl1 = ExtensionLoader.getExtensionLoader(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            System.out.println(impl1);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can not create adaptive extension interface com.alibaba.dubbo.common.extensionloader.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method on extension com.alibaba.dubbo.common.extensionloader.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
        // report same error when get is invoked for multiple times
        try {
            ExtensionLoader.getExtensionLoader(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can not create adaptive extension interface com.alibaba.dubbo.common.extensionloader.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method on extension com.alibaba.dubbo.common.extensionloader.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
    }

    /**
     * 调用 扩展接口中的 非 自适配 即不带 @Adaptive注解的方法。 将会抛出异常
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNotAdaptiveMethod() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        try {
            //调用非自适配接口 bang将会出异常，见catch中的错误信息
            String bang = ext.bang(url, 33);
            System.out.println(bang);
            fail();
        } catch (UnsupportedOperationException expected) {
            //method public abstract java.lang.String com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt.bang(com.alibaba.dubbo.common.URL,int) of interface com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt is not adaptive method!
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt is not adaptive method!"));
        }
    }

    /**
     *
     * 调用没有URL参数的方法 ，将会抛出异常，连URL（总线都没了） 还怎么玩dubbo???????哈哈
     * 必须抛出异常啊
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoUrlAttribute() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(NoUrlParamExt.class).getAdaptiveExtension();
            fail();
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("fail to create adaptive class for interface "));
            assertThat(expected.getMessage(), containsString(": not found url parameter or url attribute in parameters of method "));
        }
    }

    /**
     * 使用 UrlHolder 也可以完成自动寻找实现类的功能。 UrlHolder 直译为总线持有者，在寻找实现类时候肯定还是获取到URL (即总线) 然后找到protocol
     * 或者addParameter中设置的参数，来进行实现类的获取的。
     *
     * @throws Exception
     */
    @Test
    public void test_urlHolder_getAdaptiveExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        map.put("ext2", "impl1");
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        String echo = ext.echo(holder, "haha");
        assertEquals("Ext2Impl1-echo", echo);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void test_urlHolder_getAdaptiveExtension_noExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            //Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext2.Ext2) name from url(p1://1.2.3.4:1010/path1) use keys([ext2])
            assertThat(expected.getMessage(), containsString("Fail to get extension("));
        }

        url = url.addParameter("ext2", "XXX");
        holder.setUrl(url);
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {

            //No such extension com.alibaba.dubbo.common.extensionloader.ext2.Ext2 by name XXX
            //为啥????????? 因为 Ext2接口中 @SPI没配置值呀 就算有配置文件，里边配了几个实现类 ，但是@SPI没配置值也不行，加载不到实现类。
            assertThat(expected.getMessage(), containsString("No such extension"));
        }
    }

    /**
     * 当自适应方法 echo使用 UrlHolder作为形参，并且调用者传入null时候的情况
     *
     * @throws Exception
     */
    @Test
    public void test_urlHolder_getAdaptiveExtension_UrlNpe() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder argument == null", e.getMessage());
        }

        try {
            ext.echo(new UrlHolder(), "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder argument getUrl() == null", e.getMessage());
        }
    }

    /**
     * 当 UrlHolder作为形参，且在接口方法上没配置  @Adaptive注解的场景。
     * @throws Exception
     */
    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNotAdativeMethod() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        try {
            ext.bang(url, 33);
            fail();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface com.alibaba.dubbo.common.extensionloader.ext2.Ext2 is not adaptive method!"));
        }
    }

    /**
     * 测试找不到扩展接口的实现类的场景
     *
     * @throws Exception
     */
    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNameNotProvided() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");

        //没有设置 addParameter 的信息，找不到对应的试下
        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        try {
            String impl1 = ext.echo(holder, "impl1");
            System.out.println(impl1);
            fail();
        } catch (IllegalStateException expected) {
            //Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext2.Ext2) name from url(p1://1.2.3.4:1010/path1)   use keys([ext2] is ok)
            assertThat(expected.getMessage(), containsString("Fail to get extension("));
        }

        //故意添加一个 没有的key，导致找不到实现
        url = url.addParameter("key1", "impl1");//如果此处使用 key1将会正常获取到Ext2的实现类 impl1， 并正常返回内容
        holder.setUrl(url);
        try {
            String haha = ext.echo(holder, "haha");
            System.out.println(haha);
            fail();
        } catch (IllegalStateException expected) {
            //Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext2.Ext2) name from url(p1://1.2.3.4:1010/path1?key1=impl1) use keys([ext2] is ok)
            assertThat(expected.getMessage(), containsString("Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext2.Ext2) name from url"));
        }
    }

    /**
     * 测试自动注入依赖 ，以及(调用时候) 如何选择依赖的实现 (URL中设置的)。
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_inject() throws Exception {
        LogUtil.start();
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");

        //指定 Ext6接口 调用时候的实现是谁 注意这里为什么是 ext6呢？ 因为 @Adaptive注解上没有配置key,dubbo会默认使用类名
        /**
         *
         * @Adaptive
         *
         * 该注解也可以传入value参数，是一个数组。我们在代码清单4.9中可以看到，Adaptive可
         * 以传入多个key值，在初始化Adaptive注解的接口时，会先对传入的URL进行key值匹配，第
         * 一个key没匹配上则匹配第二个，以此类推。直到所有的key匹配完毕，如果还没有匹配到， 则会使用“驼峰规则”匹配，如果也没匹配到，则会抛出IllegalStateException异常。
         *
         * 什么是"驼峰规则”呢？如果包装类（Wrapper 没有用Adaptive指定key值，则Dubbo
         * 会自动把接口名称根据驼峰大小写分开，并用符号连接起来，以此来作为默认实现类的名
         * 称，如 org.apache.dubbo.xxx.YyylnvokerWpappep 中的 YyylnvokerWrapper 会被转换为
         * yyy.invoker.wrapper
         *
         */
        url = url.addParameters("ext6", "impl1");
        //指定 Ext6接口的依赖(setExt1()方法) 的实现是谁 至于为什么是 simple.ext 见 上边的注释即可 。注意
        //不指定的话 注入的依赖类 默认是impl1  , 注意 impl1 impl2 impl3是 在配置文件中的key 。
        url = url.addParameters("simple.ext", "impl3");

        System.out.println(ext.echo(url, "ha"));
        assertEquals("Ext6Impl1-echo-Ext1Impl1-echo", ext.echo(url, "ha"));

        Assert.assertTrue("can not find error.", LogUtil.checkNoError());
        LogUtil.stop();
        //这次指定 impl2为依赖的实现类
        url = url.addParameters("simple.ext", "impl2");
        assertEquals("Ext6Impl1-echo-Ext1Impl2-echo", ext.echo(url, "ha"));

    }

    /**
     * 直接根据 ExtensionLoader的 getExtensionLoader获取 ExtensionLoader 然后通过getExtension获取具体的实现实例。
     *
     * @throws Exception
     */
    @Test
    public void test_getAdaptiveExtension_InjectNotExtFail() throws Exception {

        //直接指定名称获取实现类
        //(前提是先根据Ext6.class获取到 ExtensionLoader) 在根据name获取具体的实现
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getExtension("impl2");

        Ext6Impl2 impl = (Ext6Impl2) ext;
        assertNull(impl.getList());
    }
}