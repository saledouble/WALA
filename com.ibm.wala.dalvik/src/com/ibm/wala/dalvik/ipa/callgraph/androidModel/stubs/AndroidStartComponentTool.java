/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;

import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

import com.ibm.wala.util.strings.Atom;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StarterFlags;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StartInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Grab and set data of AndroidClasses.
 *
 *  This class is only used by AndroidModel.getMethodAs() as it got a bit lengthy.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-22
 */
public class AndroidStartComponentTool {
    private static Logger logger = LoggerFactory.getLogger(AndroidStartComponentTool.class);
    
    private final IClassHierarchy cha;
    private final MethodReference asMethod;
    private final Set<StarterFlags> flags;
    private final TypeReference caller;
    private final TypeSafeInstructionFactory instructionFactory;
    private final ParameterAccessor acc;
    private final SSAValueManager pm;
    private final VolatileMethodSummary redirect;
    private final Parameter self;
    private final StartInfo info;
    private final CGNode callerNd;
    private AndroidTypes.AndroidContextType callerContext;

    public AndroidStartComponentTool(final IClassHierarchy cha, final MethodReference asMethod, final Set<StarterFlags> flags,
            final TypeReference caller, final TypeSafeInstructionFactory instructionFactory, final ParameterAccessor acc,
            final SSAValueManager pm, final VolatileMethodSummary redirect, final Parameter self,
            final StartInfo info, final CGNode callerNd) {
        
        if (cha == null) {
            throw new IllegalArgumentException("cha may not be null");
        }
        if (asMethod == null) {
            throw new IllegalArgumentException("asMethod may not be null");
        }
        if (flags == null) {
            throw new IllegalArgumentException("Flags may not be null");
        }
        if ((caller == null) && (! flags.contains(StarterFlags.CONTEXT_FREE))) {
            throw new IllegalArgumentException("Caller may not be null if StarterFlags.CONTEXT_FREE is not set. Flags: " + flags);
        }
        if (instructionFactory == null) {
            throw new IllegalArgumentException("The instructionFactory may not be null");
        }
        if (acc == null) {
            throw new IllegalArgumentException("acc may not be null");
        }
        if (pm == null) {
            throw new IllegalArgumentException("pm may not be null");
        }
        if (redirect == null) {
            throw new IllegalArgumentException("self may not be null");
        }
        if (info == null) {
            throw new IllegalArgumentException("info may not be null");
        }
        //if ((callerNd == null) && (! flags.contains(StarterFlags.CONTEXT_FREE))) {
        //    throw new IllegalArgumentException("CallerNd may not be null if StarterFlags.CONTEXT_FREE is not set. Flags: " + flags);
        //}


        logger.debug("Starting Component {} from {} ", info, callerNd);
        this.cha = cha;
        this.asMethod = asMethod;
        this.flags = flags;
        this.caller = caller;
        this.instructionFactory = instructionFactory;
        this.acc = acc;
        this.pm = pm;
        this.redirect = redirect;
        this.self = self;
        this.info = info;
        this.callerNd = callerNd;
    }

    public void attachActivities(Set<? extends SSAValue> activities, SSAValue application, SSAValue thread, SSAValue context,
            SSAValue iBinderToken, SSAValue intent) {
        // call: final void Activity.attach(Context context, ActivityThread aThread, Instrumentation instr, IBinder token,
        //    Application application, Intent intent, ActivityInfo info, CharSequence title, 
        //    Activity parent, String id, Object lastNonConfigurationInstance,
        //    Configuration config)
        
        final SSAValue nullInstrumentation;
        {
            nullInstrumentation = pm.getUnmanaged(AndroidTypes.Instrumentation, "nullInstrumentation");
            this.redirect.addConstant(nullInstrumentation.getNumber(), new ConstantValue(null));
            nullInstrumentation.setAssigned();
        }
        final SSAValue nullInfo;
        {
            nullInfo = pm.getUnmanaged(AndroidTypes.ActivityInfo, "nullInfo");
            this.redirect.addConstant(nullInfo.getNumber(), new ConstantValue(null));
            nullInfo.setAssigned();
        }
        final SSAValue title;
        {
            title = pm.getUnmanaged(TypeReference.JavaLangString, "title");   // XXX CharSequence
            this.redirect.addConstant(title.getNumber(), new ConstantValue("title"));
            title.setAssigned();
        }
        final SSAValue nullParent;
        {
            nullParent = pm.getUnmanaged(AndroidTypes.Activity, "nullParent");   
            this.redirect.addConstant(nullParent.getNumber(), new ConstantValue(null));
            nullParent.setAssigned();
        }
        final SSAValue nullConfigInstance;
        {
            final TypeName cName = TypeName.string2TypeName("Landroid/app/Activity$NonConfigurationInstances");
            final TypeReference type = TypeReference.findOrCreate(com.ibm.wala.types.ClassLoaderReference.Primordial, cName);
            nullConfigInstance = pm.getUnmanaged(type, "noState");   
            this.redirect.addConstant(nullConfigInstance.getNumber(), new ConstantValue(null));
            nullConfigInstance.setAssigned();
        }
         final SSAValue nullConfiguration;
        {
            nullConfiguration = pm.getUnmanaged(AndroidTypes.Configuration, "nullConfig");   
            this.redirect.addConstant(nullConfiguration.getNumber(), new ConstantValue(null));
            nullConfiguration.setAssigned();
        }
       
        final Descriptor desc = Descriptor.findOrCreate(new TypeName[] {
                AndroidTypes.ContextName,
                AndroidTypes.ActivityThreadName,
                AndroidTypes.InstrumentationName,
                AndroidTypes.IBinderName,
                AndroidTypes.ApplicationName,
                AndroidTypes.IntentName,
                AndroidTypes.ActivityInfoName,
                TypeName.string2TypeName("Ljava/lang/CharSequence"),
                AndroidTypes.ActivityName,
                TypeReference.JavaLangString.getName(),
                TypeName.string2TypeName("Landroid/app/Activity$NonConfigurationInstances"),
                AndroidTypes.ConfigurationName }, TypeReference.VoidName);
        final Selector mSel = new Selector(Atom.findOrCreateAsciiAtom("attach"), desc);
        final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.Activity, mSel);
        final List<SSAValue> params = new ArrayList<SSAValue>(13);
        params.add(null);   // activity
        params.add(context);
        params.add(thread);
        params.add(nullInstrumentation);
        params.add(iBinderToken);
        params.add(application);
        params.add(intent);
        params.add(nullInfo);
        params.add(title);
        params.add(nullParent);
        params.add(title);
        params.add(nullConfigInstance);
        params.add(nullConfiguration);

        for (final SSAValue activity: activities) {
            final int callPC = redirect.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = pm.getException();
            params.set(0, activity);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, params, exception, site);
            redirect.addStatement(invokation);    
        }
    }


    public AndroidTypes.AndroidContextType typeOfCallerContext() {
        return this.callerContext;
    }

    /**
     *  Fetches the context of the caller.
     *
     *  @return A new SSAValue representing the androidContext (may be null!).  // XXX 
     */
    public SSAValue fetchCallerContext() {
        /*if (flags.contains(StarterFlags.CONTEXT_FREE)) {
            logger.warn("Asking for context when Context-Free");
            return null;    // XXX: Return a synthetic null?
        }*/
        
        logger.debug("Fetching caller context...");
        final SSAValue androidContext;
        if (caller == null) {
            return null;
        } else if (caller.getName().equals(AndroidTypes.ContextWrapperName)) {
            { // Fetch ContextWrapperName.mBase => androidContext
                androidContext = pm.getUnmanaged(AndroidTypes.Context, "callerContext");
                logger.debug("Fetching ContextWrapperName.mBase");

                final FieldReference mBaseRef = FieldReference.findOrCreate(AndroidTypes.ContextWrapper, Atom.findOrCreateAsciiAtom("mBase"),
                        AndroidTypes.Context);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, androidContext, self, mBaseRef);
                redirect.addStatement(getInst);

                // TODO: somehow dispatch on type of mBase?
                this.callerContext = AndroidTypes.AndroidContextType.CONTEXT_IMPL;
                logger.info("Caller has android-context type: ContextWrapper(ContextImpl)");
                return androidContext;
            }
        } else if (caller.getName().equals(AndroidTypes.ContextImplName)) {
            { // self is already the right context
                androidContext = self;
                this.callerContext = AndroidTypes.AndroidContextType.CONTEXT_IMPL;
                logger.info("Caller has android-context type: ContextImpl");
                return androidContext;
            }
        } else if (caller.getName().equals(AndroidTypes.ActivityName)) {
            // We don't need it for now - TODO grab anyway
            androidContext = null;
            this.callerContext = AndroidTypes.AndroidContextType.ACTIVITY;
            logger.info("Caller has android-context type: Activity");
            return androidContext;
        } else if (caller.equals(AndroidModelClass.ANDROID_MODEL_CLASS)) {
            // TODO: Return something useful
            this.callerContext = AndroidTypes.AndroidContextType.USELESS;
            return null;
        } else if (caller.getName().equals(AndroidTypes.BridgeContextName)) {
            // XXX ???
            androidContext = self;
            this.callerContext = AndroidTypes.AndroidContextType.CONTEXT_BRIDGE;
            logger.info("Caller has android-context type: BridgeContext");
            return androidContext;
        } else {
            throw new UnsupportedOperationException("Can not handle the callers android-context of " + caller);
        }
    }


    /**
     *  Fetch the permissions to start the component with.
     *
     *  Fetching depends on StarterFlags.QUENCH_PERMISSIONS, XXX 
     *
     *  @return an iBinder
     *  @throws UnsupportedOperationException when fetching is not supported with the current settings
     */
    public SSAValue fetchIBinder(SSAValue androidContext) {
        logger.debug("Fetching context to use for call...");
        final SSAValue iBinder = pm.getUnmanaged(AndroidTypes.IBinder, "foreignIBinder");

        if (flags.contains(StarterFlags.CONTEXT_FREE)) {
            // TODO: Can we do somethig?
            return null;
        } else if (flags.contains(StarterFlags.QUENCH_PERMISSIONS)) {
            // If this flag is set the given asMethod has a IntentSender-Parameter

            final Parameter intentSender = acc.firstOf(AndroidTypes.IntentSenderName);
            assert (intentSender != null) : "Unable to look up the IntentSender-Object";
            assert (intentSender.getNumber() == 2) : "The IntentSender-Object was not located at SSA-Number 2. This may be entirely " +
                    "ok! I left this assertion to ashure the ParameterAccessor does its job right.";

            // retreive the IBinder: IIntentSender.asBinder()
            final SSAValue iIntentSender = pm.getUnmanaged(AndroidTypes.IIntentSender, "iIntentSender");
            { // call IIntentSender IntentSender.getTarget()
                final int callPC = redirect.getNextProgramCounter();
                final Selector mSel = Selector.make("getTarget()Landroid/content/IIntentSender;");
                final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.IntentSender, mSel);
                final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
                final SSAValue exception = pm.getException();
                final List<SSAValue> params = new ArrayList<SSAValue>(1);
                params.add(intentSender);
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, iIntentSender, params, exception, site);
                redirect.addStatement(invokation);
            }

            { // call IBinder IIntentSender.asBinder()
                final int callPC = redirect.getNextProgramCounter();
                final Selector mSel = Selector.make("asBinder()Landroid/os/IBinder;");
                final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.IntentSender, mSel);
                final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
                final SSAValue exception = pm.getException();
                final List<SSAValue> params = new ArrayList<SSAValue>(1);
                params.add(iIntentSender);
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, iBinder, params, exception, site);
                redirect.addStatement(invokation);
            }
       
            logger.info("The context to use for the call is from an IBinder");
            return iBinder;
        } else if (caller.getName().equals(AndroidTypes.ActivityName)) {
            
            // The IBinder is Activity.mMainThread.getApplicationThread()   // TODO: Verify
            final SSAValue mMainThread = pm.getUnmanaged(AndroidTypes.ActivityThread, "callersMainThred");
            { // Fetch mMainthred
                final int instPC = redirect.getNextProgramCounter();
                final FieldReference mMainThreadRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mMainThread"),
                        AndroidTypes.ActivityThread);
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, mMainThread, self, mMainThreadRef);
                redirect.addStatement(getInst);
            }

            /*{ // DEBUG
                final com.ibm.wala.classLoader.IClass activityThread = cha.lookupClass(AndroidTypes.ActivityThread);
                assert (activityThread != null);
                for (com.ibm.wala.classLoader.IMethod m : activityThread.getDeclaredMethods()) {
                    System.out.println(m);
                }
            } // */

            { // Call getApplicationThread() on it
                final int callPC = redirect.getNextProgramCounter();
                final Selector mSel = Selector.make("getApplicationThread()Landroid/app/ActivityThread$ApplicationThread;");
                final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.ActivityThread, mSel);
                final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
                final SSAValue exception = pm.getException();
                final List<SSAValue> params = new ArrayList<SSAValue>(1);
                params.add(mMainThread);
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, iBinder,  params, exception, site);
                redirect.addStatement(invokation);
            } // */
        
            logger.info("The context (IBinder) to use for the call is the one from the calling activity");
            return iBinder;
        } else if (this.callerContext == AndroidTypes.AndroidContextType.CONTEXT_IMPL) {
            // For bindService its mActivityToken - TODO: For the rest? 
            // startActivity uses mMainThread.getApplicationThread()

            { // read mActivityToken -> iBinder
                logger.debug("Fetching ContextImpl.mActivityToken to iBinder");

                final FieldReference mActivityTokenRef = FieldReference.findOrCreate(AndroidTypes.ContextImpl,
                        Atom.findOrCreateAsciiAtom("mActivityToken"), AndroidTypes.IBinder);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, iBinder, androidContext, mActivityTokenRef);
                redirect.addStatement(getInst);
            }
        
            logger.info("The context (IBinder) to use for the call is the one from the caller");
            return iBinder;
        } else if (this.callerContext == AndroidTypes.AndroidContextType.CONTEXT_BRIDGE) {
            // TODO: Return something useful
            logger.error("Not Implemented: Fetch an IBinder from a BridgeContext.");
            return null;
        } else if (caller.equals(AndroidModelClass.ANDROID_MODEL_CLASS)) {
            // TODO: Return something useful
            return null;
        } else {
            throw new UnsupportedOperationException("No implementation on how to extract an iBinder from a " + caller);
        }
    }

    /**
     *  Set the iBinder in the callee.
     */
    public void assignIBinder(SSAValue iBinder, List<? extends SSAValue> allActivities) {
        if (iBinder == null) {
            // TODO: Some day we may throe here...
            return;
        }
        logger.info("Assigning the iBinder");
        // TODO: Use Phi?
        for (SSAValue activity : allActivities) {
            logger.debug("\tto: {}", activity);
            //final int callPC = redirect.getNextProgramCounter();

            final FieldReference mTokenRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mToken"),
                    AndroidTypes.IBinder);
            final int instPC = redirect.getNextProgramCounter();
            final SSAInstruction putInst = instructionFactory.PutInstruction(instPC, activity, iBinder, mTokenRef);
            redirect.addStatement(putInst);
        }
    }


    /**
     *  Grab mResultCode and mResultData.
     *
     *  This data is used to call onActivityResult of the caller.
     */
    public void fetchResults(List<? super SSAValue> resultCodes, List<? super SSAValue> resultData, 
            List<? extends SSAValue> allActivities) {
        for (SSAValue activity : allActivities) { 
            final SSAValue tmpResultCode = pm.getUnmanaged(TypeReference.Int, "mResultCode");
            { // Fetch mResultCode
                //redirect.setLocalName(tmpResultCode, "gotResultCode");
                logger.debug("Fetching ResultCode");

                final FieldReference mResultCodeRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mResultCode"),
                        TypeReference.Int);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, tmpResultCode, activity, mResultCodeRef);
                redirect.addStatement(getInst);
            }

            final SSAValue tmpResultData = pm.getUnmanaged(AndroidTypes.Intent, "mResultData");
            { // Fetch mResultData
                //redirect.setLocalName(tmpResultData, "gotResultData");
                logger.debug("Fetching Result data");

                final FieldReference mResultDataRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mResultData"),
                        AndroidTypes.Intent);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, tmpResultData, activity, mResultDataRef);
                redirect.addStatement(getInst);
            } // */

            resultCodes.add(tmpResultCode);
            resultData.add(tmpResultData);
        } // End: for all activities */

        assert (resultCodes.size() == resultData.size());
    }

    /**
     *  Add Phi (if necessary) - not if only one from.
     */
    public SSAValue addPhi(List<? extends SSAValue> from) {
        logger.debug("Add Phi({})", from);

        if (from.size() == 1) {
            return from.get(0);
        } else {
            final SSAValue retVal = this.pm.getUnmanaged(from.get(0).getType(), "forPhi");
            final int phiPC = redirect.getNextProgramCounter();
            final SSAInstruction phi = instructionFactory.PhiInstruction(phiPC, retVal, from);
            this.redirect.addStatement(phi);
            return retVal;
        }
    }
}
