'use client';

import useSWR from 'swr';
import { dashboardApi, announcementsApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Home,
  Users,
  Receipt,
  MessageSquareWarning,
  AlertTriangle,
  Megaphone,
  TrendingUp,
  AlertCircle,
} from 'lucide-react';
import type { DashboardStats, Announcement } from '@/lib/types';

function StatCard({
  title,
  value,
  description,
  icon: Icon,
  trend,
}: {
  title: string;
  value: string | number;
  description?: string;
  icon: React.ComponentType<{ className?: string }>;
  trend?: 'up' | 'down' | 'neutral';
}) {
  return (
    <Card className="bg-card border-border">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold text-foreground">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground mt-1 flex items-center gap-1">
            {trend === 'up' && <TrendingUp className="h-3 w-3 text-success" />}
            {trend === 'down' && <AlertCircle className="h-3 w-3 text-destructive" />}
            {description}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function AnnouncementCard({ announcement }: { announcement: Announcement }) {
  const typeColors: Record<string, string> = {
    URGENT: 'bg-destructive/10 text-destructive border-destructive/20',
    GENERAL: 'bg-primary/10 text-primary border-primary/20',
    MAINTENANCE: 'bg-warning/10 text-warning border-warning/20',
    EVENT: 'bg-success/10 text-success border-success/20',
    MEETING: 'bg-chart-5/10 text-chart-5 border-chart-5/20',
  };

  return (
    <div className="p-4 rounded-lg border border-border bg-card/50">
      <div className="flex items-start justify-between gap-2">
        <h4 className="font-medium text-foreground">{announcement.title}</h4>
        <Badge variant="outline" className={typeColors[announcement.type] || ''}>
          {announcement.type}
        </Badge>
      </div>
      <p className="text-sm text-muted-foreground mt-2 line-clamp-2">{announcement.content}</p>
      <p className="text-xs text-muted-foreground mt-2">
        {new Date(announcement.createdAt).toLocaleDateString()}
      </p>
    </div>
  );
}

export default function DashboardPage() {
  const { user, isAdmin } = useAuth();
  
  const { data: stats, isLoading: statsLoading } = useSWR<DashboardStats>(
    'dashboard-stats',
    () => dashboardApi.getStats()
  );

  const { data: announcements, isLoading: announcementsLoading } = useSWR<Announcement[]>(
    'active-announcements',
    () => announcementsApi.getActive()
  );

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">
          Welcome back, {user?.name?.split(' ')[0]}
        </h1>
        <p className="text-muted-foreground">
          {"Here's what's happening in your society today."}
        </p>
      </div>

      {statsLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="bg-card border-border animate-pulse">
              <CardHeader className="pb-2">
                <div className="h-4 bg-muted rounded w-24"></div>
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-muted rounded w-16"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {isAdmin && (
            <>
              <StatCard
                title="Total Flats"
                value={stats?.totalFlats || 0}
                description={`${stats?.occupiedFlats || 0} occupied`}
                icon={Home}
              />
              <StatCard
                title="Total Residents"
                value={stats?.totalResidents || 0}
                icon={Users}
              />
            </>
          )}
          <StatCard
            title="Pending Bills"
            value={stats?.pendingBills || 0}
            description={stats?.overdueAmount ? formatCurrency(stats.overdueAmount) + ' overdue' : undefined}
            icon={Receipt}
            trend={stats?.overdueAmount && stats.overdueAmount > 0 ? 'down' : 'neutral'}
          />
          <StatCard
            title="Open Complaints"
            value={stats?.openComplaints || 0}
            icon={MessageSquareWarning}
          />
          <StatCard
            title="Open Issues"
            value={stats?.openIssues || 0}
            icon={AlertTriangle}
          />
          <StatCard
            title="Announcements"
            value={stats?.recentAnnouncements || 0}
            description="Active this week"
            icon={Megaphone}
          />
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="bg-card border-border">
          <CardHeader>
            <CardTitle className="text-foreground">Recent Announcements</CardTitle>
            <CardDescription>Latest updates from the society</CardDescription>
          </CardHeader>
          <CardContent>
            {announcementsLoading ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="p-4 rounded-lg border border-border animate-pulse">
                    <div className="h-4 bg-muted rounded w-3/4 mb-2"></div>
                    <div className="h-3 bg-muted rounded w-full"></div>
                  </div>
                ))}
              </div>
            ) : announcements && announcements.length > 0 ? (
              <div className="space-y-4">
                {announcements.slice(0, 3).map((announcement) => (
                  <AnnouncementCard key={announcement.id} announcement={announcement} />
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground text-center py-8">No active announcements</p>
            )}
          </CardContent>
        </Card>

        <Card className="bg-card border-border">
          <CardHeader>
            <CardTitle className="text-foreground">Quick Actions</CardTitle>
            <CardDescription>Common tasks you can perform</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-3 sm:grid-cols-2">
              <a
                href="/dashboard/complaints"
                className="flex items-center gap-3 p-4 rounded-lg border border-border bg-card/50 hover:bg-accent transition-colors"
              >
                <MessageSquareWarning className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium text-foreground">File Complaint</p>
                  <p className="text-xs text-muted-foreground">Report an issue</p>
                </div>
              </a>
              <a
                href="/dashboard/issues"
                className="flex items-center gap-3 p-4 rounded-lg border border-border bg-card/50 hover:bg-accent transition-colors"
              >
                <AlertTriangle className="h-5 w-5 text-warning" />
                <div>
                  <p className="font-medium text-foreground">Report Issue</p>
                  <p className="text-xs text-muted-foreground">Maintenance request</p>
                </div>
              </a>
              <a
                href="/dashboard/billing"
                className="flex items-center gap-3 p-4 rounded-lg border border-border bg-card/50 hover:bg-accent transition-colors"
              >
                <Receipt className="h-5 w-5 text-success" />
                <div>
                  <p className="font-medium text-foreground">View Bills</p>
                  <p className="text-xs text-muted-foreground">Check your dues</p>
                </div>
              </a>
              <a
                href="/dashboard/announcements"
                className="flex items-center gap-3 p-4 rounded-lg border border-border bg-card/50 hover:bg-accent transition-colors"
              >
                <Megaphone className="h-5 w-5 text-chart-5" />
                <div>
                  <p className="font-medium text-foreground">Announcements</p>
                  <p className="text-xs text-muted-foreground">Society updates</p>
                </div>
              </a>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
