package org.openmrs.client.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.openmrs.client.R;
import org.openmrs.client.activities.fragments.FindPatientLastViewedFragment;
import org.openmrs.client.activities.fragments.PatientDetailsFragment;
import org.openmrs.client.activities.fragments.PatientDiagnosisFragment;
import org.openmrs.client.activities.fragments.PatientVisitsFragment;
import org.openmrs.client.activities.fragments.PatientVitalsFragment;
import org.openmrs.client.application.OpenMRS;
import org.openmrs.client.dao.PatientDAO;
import org.openmrs.client.models.Patient;
import org.openmrs.client.net.FindVisitsManager;
import org.openmrs.client.net.SynchronizePatientManager;
import org.openmrs.client.utilities.ApplicationConstants;
import org.openmrs.client.utilities.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatientDashboardActivity extends ACBaseActivity implements ActionBar.TabListener {

    private Patient mPatient;

    private ViewPager mViewPager;
    private PatientDashboardPagerAdapter mPatientDashboardPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.patient_dashboard_layout);

        List<TabHost> tabHosts = new ArrayList<TabHost>(Arrays.asList(
                new TabHost(TabHost.DETAILS_TAB_POS, getString(R.string.patient_scroll_tab_details_label)),
                new TabHost(TabHost.DIAGNOSIS_TAB_POS, getString(R.string.patient_scroll_tab_diagnosis_label)),
                new TabHost(TabHost.VISITS_TAB_POS, getString(R.string.patient_scroll_tab_visits_label)),
                new TabHost(TabHost.VITALS_TAB_POS, getString(R.string.patient_scroll_tab_vitals_label))
        ));

        Bundle patientBundle = getIntent().getExtras();
        mPatient = new PatientDAO().findPatientByUUID(patientBundle.getString(ApplicationConstants.BundleKeys.PATIENT_ID_BUNDLE));

        mPatientDashboardPagerAdapter = new PatientDashboardPagerAdapter(getSupportFragmentManager(), tabHosts);
        initViewPager();
    }

    private void initViewPager() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPatientDashboardPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (TabHost tabHost : mPatientDashboardPagerAdapter.getTabHosts()) {
            actionBar.addTab(actionBar.newTab()
                    .setText(tabHost.getTabLabel())
                    .setTabListener(this));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.patients_menu, menu);
        getSupportActionBar().setTitle(mPatient.getDisplay());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_synchronize:
                synchronizePatient();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void synchronizePatient() {
        OpenMRS.getInstance().getOpenMRSLogger().d("SYNCHRONIZE STARTED!");
        SynchronizePatientManager spm = new SynchronizePatientManager(this);
        spm.getFullPatientData(mPatient.getUuid());
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    public void updatePatientsData(Patient patient) {
        if (new PatientDAO().updatePatient(mPatient.getId(), patient)) {

            ToastUtil.showShortToast(this,
                    ToastUtil.ToastType.SUCCESS,
                    R.string.synchronize_patient_successful);

            mPatient = new PatientDAO().findPatientByUUID(mPatient.getUuid());
            PatientDetailsFragment fragment = (PatientDetailsFragment) getSupportFragmentManager().getFragments().get(PatientDashboardActivity.TabHost.DETAILS_TAB_POS);
            fragment.reloadPatientData(patient);
            FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
            fragTransaction.detach(fragment);
            fragTransaction.attach(fragment);
            fragTransaction.commit();
        } else {
            stopLoader();
        }
    }

    public void stopLoader() {
        ToastUtil.showShortToast(this,
                ToastUtil.ToastType.ERROR,
                R.string.synchronize_patient_error);
    }

    public class PatientDashboardPagerAdapter extends FragmentPagerAdapter {
        private List<TabHost> mTabHosts;

        public PatientDashboardPagerAdapter(FragmentManager fm, List<TabHost> tabHosts) {
            super(fm);
            mTabHosts = tabHosts;
        }

        public List<TabHost> getTabHosts() {
            return mTabHosts;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case TabHost.DETAILS_TAB_POS:
                    return PatientDetailsFragment.newInstance(mPatient);
                case TabHost.DIAGNOSIS_TAB_POS:
                    return PatientDiagnosisFragment.newInstance(mPatient.getId());
                case TabHost.VISITS_TAB_POS:
                    return PatientVisitsFragment.newInstance(mPatient.getId());
                case TabHost.VITALS_TAB_POS:
                    return PatientVitalsFragment.newInstance(mPatient.getId());
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return mTabHosts.size();
        }

    }

    private final class TabHost {
        public static final int DETAILS_TAB_POS = 0;
        public static final int DIAGNOSIS_TAB_POS = 1;
        public static final int VISITS_TAB_POS = 2;
        public static final int VITALS_TAB_POS = 3;

        private Integer mTabPosition;
        private String mTabLabel;

        private TabHost(Integer position, String tabLabel) {
            mTabPosition = position;
            mTabLabel = tabLabel;
        }

        public Integer getTabPosition() {
            return mTabPosition;
        }

        public String getTabLabel() {
            return mTabLabel;
        }
    }

}
