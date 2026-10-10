import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { Sidebar, type SectionId } from './components/Sidebar';
import { SystemOverview } from './components/sections/SystemOverview';
import { LiveAnalysis } from './components/sections/LiveAnalysis';
import { PipelineTrace } from './components/sections/PipelineTrace';
import { EarbBenchmark } from './components/sections/EarbBenchmark';
import { ModelPerformance } from './components/sections/ModelPerformance';
import { BaselineComparison } from './components/sections/BaselineComparison';
import { AblationResults } from './components/sections/AblationResults';
import { PrivacyUtility } from './components/sections/PrivacyUtility';
import { FailureAnalysis } from './components/sections/FailureAnalysis';
import { NetworkActivity } from './components/sections/NetworkActivity';
import { ModelHealth } from './components/sections/ModelHealth';
import { LiveDeviceRelayPanel } from './components/sections/LiveDeviceRelayPanel';

import type {
  SystemOverviewData,
  EarbData,
  BaseArtifact,
  EvaluationResults,
  FailureAnalysisData,
  NetworkAuditEntry,
  AnalysisResponse,
  ModelHealthData,
} from './types';

import {
  fetchSystemOverview,
  fetchEarbData,
  fetchArtifacts,
  fetchEvaluationResults,
  fetchFailureAnalysis,
  fetchNetworkActivity,
  fetchModelHealth,
} from './services/api';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState<SectionId>('overview');
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);

  const [overview, setOverview] = useState<SystemOverviewData | null>(null);
  const [earb, setEarb] = useState<EarbData | null>(null);
  const [artifacts, setArtifacts] = useState<BaseArtifact[]>([]);
  const [results, setResults] = useState<EvaluationResults | null>(null);
  const [failures, setFailures] = useState<FailureAnalysisData | null>(null);
  const [networkLogs, setNetworkLogs] = useState<NetworkAuditEntry[]>([]);
  const [modelHealth, setModelHealth] = useState<ModelHealthData | null>(null);

  const [latestAnalysisResult, setLatestAnalysisResult] = useState<AnalysisResponse | null>(null);

  const loadAllData = async () => {
    try {
      setRefreshing(true);
      const [ov, eb, arts, res, fl, net, mh] = await Promise.allSettled([
        fetchSystemOverview(),
        fetchEarbData(),
        fetchArtifacts(),
        fetchEvaluationResults(),
        fetchFailureAnalysis(),
        fetchNetworkActivity(),
        fetchModelHealth(),
      ]);

      if (ov.status === 'fulfilled') setOverview(ov.value);
      if (eb.status === 'fulfilled') setEarb(eb.value);
      if (arts.status === 'fulfilled') setArtifacts(arts.value);
      if (res.status === 'fulfilled') setResults(res.value);
      if (fl.status === 'fulfilled') setFailures(fl.value);
      if (net.status === 'fulfilled') setNetworkLogs(net.value.entries);
      if (mh.status === 'fulfilled') setModelHealth(mh.value);
    } catch (err) {
      console.error('Failed to load supervisor data:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  const handleSelectSection = (id: SectionId) => {
    setActiveSection(id);
    if (id !== 'all') {
      const el = document.getElementById(`section-${id}`);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-primary)', color: 'var(--text-main)' }}>
      {/* Top Bar */}
      <Navbar
        overview={overview}
        loading={refreshing}
        onRefresh={loadAllData}
      />

      {/* Main Workspace Layout */}
      <div style={{ display: 'flex', minHeight: 'calc(100vh - 68px)' }}>
        {/* Navigation Sidebar */}
        <Sidebar
          activeSection={activeSection}
          onSelectSection={handleSelectSection}
          failureCount={
            (failures?.summary?.false_acts_count || 0) +
            (failures?.summary?.false_stops_count || 0)
          }
        />

        {/* Content Console */}
        <main style={{
          flex: 1,
          padding: '24px 32px',
          overflowY: 'auto',
          maxWidth: '1600px',
        }}>
          {activeSection === 'all' ? (
            <div>
              <LiveDeviceRelayPanel />
              <SystemOverview data={overview} loading={loading} />
              <LiveAnalysis
                artifacts={artifacts}
                latestResult={latestAnalysisResult}
                onAnalysisComplete={(res) => setLatestAnalysisResult(res)}
              />
              <PipelineTrace latestResult={latestAnalysisResult} />
              <EarbBenchmark data={earb} loading={loading} />
              <ModelPerformance overview={overview} />
              <BaselineComparison results={results} loading={loading} />
              <AblationResults results={results} loading={loading} />
              <PrivacyUtility results={results} loading={loading} />
              <FailureAnalysis data={failures} loading={loading} />
              <NetworkActivity entries={networkLogs} loading={loading} />
              <ModelHealth initialData={modelHealth} />
            </div>
          ) : (
            <div>
              {activeSection === 'overview' && (
                <SystemOverview data={overview} loading={loading} />
              )}
              {activeSection === 'relay' && (
                <LiveDeviceRelayPanel />
              )}
              {activeSection === 'live-analysis' && (
                <LiveAnalysis
                  artifacts={artifacts}
                  latestResult={latestAnalysisResult}
                  onAnalysisComplete={(res) => setLatestAnalysisResult(res)}
                />
              )}
              {activeSection === 'pipeline' && (
                <PipelineTrace latestResult={latestAnalysisResult} />
              )}
              {activeSection === 'earb' && (
                <EarbBenchmark data={earb} loading={loading} />
              )}
              {activeSection === 'models' && (
                <ModelPerformance overview={overview} />
              )}
              {activeSection === 'baselines' && (
                <BaselineComparison results={results} loading={loading} />
              )}
              {activeSection === 'ablations' && (
                <AblationResults results={results} loading={loading} />
              )}
              {activeSection === 'privacy-utility' && (
                <PrivacyUtility results={results} loading={loading} />
              )}
              {activeSection === 'failures' && (
                <FailureAnalysis data={failures} loading={loading} />
              )}
              {activeSection === 'network' && (
                <NetworkActivity entries={networkLogs} loading={loading} />
              )}
              {activeSection === 'health' && (
                <ModelHealth initialData={modelHealth} />
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default App;
